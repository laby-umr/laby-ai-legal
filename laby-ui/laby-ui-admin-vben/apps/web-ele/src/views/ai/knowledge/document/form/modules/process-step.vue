<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { ElAlert, ElButton, ElMessage, ElProgress } from 'element-plus';

import { getKnowledgeSegmentProcessList } from '#/api/ai/knowledge/segment';
import { retryKnowledgeDocumentIngest } from '#/api/ai/knowledge/document';
import {
  canRetryIngest,
  IngestStatus,
} from '#/views/ai/knowledge/document/utils/ingest';
import {
  EMBEDDING_POLL_INTERVAL_MS,
  getProcessStatusLabel,
  isEmbeddingComplete,
  mergeProcessList,
  shouldPollProcess,
} from '#/views/ai/knowledge/document/utils/process';

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['update:modelValue']);
const parent = inject('parent') as any;
const pollingTimer = ref<null | number>(null);
const processStateMap = ref<Record<number, import('#/views/ai/knowledge/document/utils/process').KnowledgeDocumentProcessState>>({});

const fileList = computed(() => props.modelValue.list || []);

function getFileState(file: any) {
  return processStateMap.value[file.id] || {
    progress: file.progress || 0,
    count: file.count || 0,
    embeddingCount: 0,
  };
}

function isProcessComplete(file: any) {
  const state = getFileState(file);
  return isEmbeddingComplete(state.progress, state.count);
}

const allProcessComplete = computed(() => {
  return fileList.value.length > 0 && fileList.value.every((file: any) => isProcessComplete(file));
});

const hasStale = computed(() =>
  fileList.value.some((file: any) => getFileState(file).stale),
);

const hasFailed = computed(() =>
  fileList.value.some(
    (file: any) => getFileState(file).ingestStatus === IngestStatus.FAILED,
  ),
);

const failedError = computed(() => {
  const file = fileList.value.find(
    (f: any) => getFileState(f).ingestStatus === IngestStatus.FAILED,
  );
  return file ? getFileState(file).ingestError : '';
});

const showRetry = computed(() =>
  fileList.value.some((f: any) =>
    canRetryIngest(getFileState(f).ingestStatus, getFileState(f).stale),
  ),
);

async function handleRetry() {
  const file = fileList.value.find((f: any) => f.id);
  if (!file?.id) {
    return;
  }
  try {
    await retryKnowledgeDocumentIngest(file.id);
    ElMessage.success('已提交重新入库');
    pollProcessList();
  } catch (e) {
    console.error(e);
  }
}

function handleComplete() {
  if (parent?.exposed?.handleBack) {
    parent.exposed.handleBack();
  }
}

function handleGoList() {
  if (parent?.exposed?.goToDocumentListAfterProcess) {
    parent.exposed.goToDocumentListAfterProcess();
  } else if (parent?.exposed?.handleBack) {
    parent.exposed.handleBack();
  }
}

function syncModelValueFromState() {
  const updatedList = fileList.value.map((file: any) => {
    const state = getFileState(file);
    return {
      ...file,
      progress: state.progress,
      count: state.count,
    };
  });
  emit('update:modelValue', {
    ...props.modelValue,
    list: updatedList,
  });
}

async function pollProcessList() {
  try {
    const documentIds = fileList.value
      .filter((item: any) => item.id)
      .map((item: any) => item.id);
    if (documentIds.length === 0) {
      return;
    }
    const result = await getKnowledgeSegmentProcessList(documentIds);
    processStateMap.value = mergeProcessList(result, processStateMap.value);
    syncModelValueFromState();

    const needPoll = fileList.value.some((file: any) =>
      shouldPollProcess(getFileState(file)),
    );
    if (needPoll) {
      pollingTimer.value = window.setTimeout(
        pollProcessList,
        EMBEDDING_POLL_INTERVAL_MS,
      );
    }
  } catch (error) {
    console.error('获取处理进度失败:', error);
    pollingTimer.value = window.setTimeout(pollProcessList, 5000);
  }
}

onMounted(() => {
  const hasProgress = fileList.value.some(
    (file: any) => file.progress != null && file.progress > 0,
  );
  if (!hasProgress) {
    const initialList = fileList.value.map((file: any) => ({
      ...file,
      progress: file.progress ?? 0,
      count: file.count ?? 0,
    }));
    emit('update:modelValue', {
      ...props.modelValue,
      list: initialList,
    });
  }
  pollProcessList();
});

onBeforeUnmount(() => {
  if (pollingTimer.value) {
    clearTimeout(pollingTimer.value);
    pollingTimer.value = null;
  }
});
</script>

<template>
  <div>
    <ElAlert
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
      title="向量化在后台进行"
      description="可点击下方「返回文档列表」随时查看进度；刷新本页后若未完成，可从列表「处理进度」再次进入。"
    />

    <ElAlert
      v-if="hasFailed"
      type="error"
      :closable="false"
      show-icon
      class="mb-4"
      title="向量化失败"
      :description="failedError || '请查看后端日志，或点击下方重新入库。'"
    />

    <ElAlert
      v-if="hasStale && !hasFailed"
      type="warning"
      :closable="false"
      show-icon
      class="mb-4"
      title="进度长时间未变化"
      description="可能因向量 API 或 Qdrant 异常导致中断。请查看后端日志，或在文档列表中禁用再启用该文档以重新向量化。"
    />

    <div class="mt-2 grid grid-cols-1 gap-3">
      <div
        v-for="(file, index) in fileList"
        :key="file.id || index"
        class="rounded-sm border border-l-4 border-l-blue-500 px-3 py-2 shadow-sm"
      >
        <div class="mb-2 flex items-center">
          <IconifyIcon icon="lucide:file-text" class="mr-2 text-blue-500" />
          <span class="flex-1 break-all text-sm text-gray-600">
            {{ file.name }}
          </span>
          <span
            class="ml-2 text-xs"
            :class="getFileState(file).stale ? 'text-amber-600' : 'text-gray-500'"
          >
            {{ getProcessStatusLabel(getFileState(file)) }}
          </span>
        </div>
        <ElProgress
          :percentage="getFileState(file).progress"
          :stroke-width="10"
          :status="isProcessComplete(file) ? 'success' : getFileState(file).stale ? 'warning' : undefined"
        />
        <div class="mt-1 text-xs text-gray-400">
          已向量化 {{ getFileState(file).embeddingCount }} / {{ getFileState(file).count || '-' }} 段
        </div>
        <div
          v-if="getFileState(file).ingestError"
          class="mt-1 text-xs text-red-500"
        >
          {{ getFileState(file).ingestError }}
        </div>
      </div>
    </div>

    <div class="mt-5 flex justify-between">
      <div class="flex gap-2">
        <ElButton @click="handleGoList">返回文档列表</ElButton>
        <ElButton v-if="showRetry" type="warning" @click="handleRetry">
          重新入库
        </ElButton>
      </div>
      <ElButton
        :type="allProcessComplete ? 'primary' : 'default'"
        :disabled="!allProcessComplete"
        @click="handleComplete"
      >
        完成
      </ElButton>
    </div>
  </div>
</template>
