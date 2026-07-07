<script setup lang="ts">
import type { PropType } from 'vue';

import { computed, getCurrentInstance, inject, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElTooltip,
} from 'element-plus';

import {
  createKnowledgeDocumentList,
  updateKnowledgeDocument,
} from '#/api/ai/knowledge/document';
import { splitContent } from '#/api/ai/knowledge/segment';

const props = defineProps({
  modelValue: {
    type: Object as PropType<any>,
    required: true,
  },
});

const emit = defineEmits(['update:modelValue']);
const parent = inject('parent', null);

const modelData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const splitLoading = ref(false);
const submitLoading = ref(false);
const currentFileIndex = ref(0);
const activeSegmentIndex = ref(0);

const currentFile = computed(() => modelData.value.list?.[currentFileIndex.value] ?? null);

const fileOptions = computed(() =>
  (modelData.value.list || []).map((file: any, index: number) => ({
    label: file.name,
    value: index,
    segmentCount: file.segments?.length,
  })),
);

const segmentSummary = computed(() => {
  const segments = currentFile.value?.segments;
  if (!segments?.length) {
    return null;
  }
  return {
    count: segments.length,
    totalTokens: segments.reduce(
      (sum: number, item: any) => sum + (item.tokens || 0),
      0,
    ),
    totalChars: segments.reduce(
      (sum: number, item: any) => sum + (item.contentLength || 0),
      0,
    ),
  };
});

const activeSegment = computed(
  () => currentFile.value?.segments?.[activeSegmentIndex.value],
);

function segmentPreview(content?: string) {
  if (!content) {
    return '';
  }
  const line = content.replace(/\s+/g, ' ').trim();
  return line.length > 72 ? `${line.slice(0, 72)}…` : line;
}

async function splitContentFile(file: any) {
  if (!file?.url) {
    ElMessage.warning('文件 URL 不存在');
    return;
  }

  splitLoading.value = true;
  try {
    file.segments = await splitContent(file.url, modelData.value.segmentMaxTokens);
    activeSegmentIndex.value = 0;
  } catch (error: any) {
    console.error('获取分段内容失败:', file, error);
    const status = error?.response?.status;
    if (status === 502 || status === 503) {
      ElMessage.error('后端服务不可用（502），请确认 Java 服务已在 48080 端口启动');
    } else if (error?.code === 'ECONNABORTED' || error?.message?.includes('timeout')) {
      ElMessage.error(
        '分段预览超时，请检查文件是否过大，或基础设施中文件 URL 是否可被后端访问',
      );
    } else {
      ElMessage.error(
        error?.message || '获取分段内容失败，请检查文件存储配置与后端日志',
      );
    }
  } finally {
    splitLoading.value = false;
  }
}

watch(currentFileIndex, async (index) => {
  activeSegmentIndex.value = 0;
  const file = modelData.value.list?.[index];
  if (file && !file.segments?.length) {
    await splitContentFile(file);
  }
});

watch(
  () => modelData.value.segmentMaxTokens,
  (value, oldValue) => {
    if (oldValue != null && value !== oldValue && currentFile.value?.segments?.length) {
      ElMessage.info('Token 数已变更，请重新预览分段');
      currentFile.value.segments = [];
    }
  },
);

async function handleRefreshPreview() {
  if (!currentFile.value) {
    ElMessage.warning('请先选择文档');
    return;
  }
  await splitContentFile(currentFile.value);
}

function handlePrevStep() {
  const parentEl = parent || getCurrentInstance()?.parent;
  if (parentEl && typeof parentEl.exposed?.goToPrevStep === 'function') {
    parentEl.exposed.goToPrevStep();
  }
}

async function handleSave() {
  const list = modelData.value.list || [];
  const missingPreview = list.find(
    (file: any) => !file.segments || file.segments.length === 0,
  );
  if (missingPreview) {
    ElMessage.warning(`请先预览「${missingPreview.name}」的分段内容`);
    return;
  }

  submitLoading.value = true;
  try {
    if (modelData.value.id) {
      await updateKnowledgeDocument({
        id: modelData.value.id,
        segmentMaxTokens: modelData.value.segmentMaxTokens,
      });
    } else {
      const data = await createKnowledgeDocumentList({
        knowledgeId: modelData.value.knowledgeId,
        segmentMaxTokens: modelData.value.segmentMaxTokens,
        list: list.map((item: any) => ({
          name: item.name,
          url: item.url,
        })),
      });
      list.forEach((document: any, index: number) => {
        document.id = data[index];
      });
    }

    ElMessage.success('已提交向量化，列表将自动刷新进度');
    const parentEl = parent || getCurrentInstance()?.parent;
    if (parentEl && typeof parentEl.exposed?.goToDocumentListAfterProcess === 'function') {
      parentEl.exposed.goToDocumentListAfterProcess();
    }
  } catch (error: any) {
    console.error('保存失败:', modelData.value, error);
  } finally {
    submitLoading.value = false;
  }
}

onMounted(async () => {
  if (!modelData.value.segmentMaxTokens) {
    modelData.value.segmentMaxTokens = 500;
  }
  if (modelData.value.list?.length && currentFile.value) {
    await splitContentFile(currentFile.value);
  }
});
</script>

<template>
  <div class="split-step">
    <div
      class="mb-4 flex flex-wrap items-end gap-x-4 gap-y-3 rounded-lg border border-border bg-muted/20 p-4"
    >
      <ElForm inline class="!mb-0 flex flex-wrap items-end gap-x-4 gap-y-2">
        <ElFormItem label="最大 Token" class="!mb-0">
          <ElInputNumber
            v-model="modelData.segmentMaxTokens"
            :min="1"
            :max="2048"
            controls-position="right"
            class="!w-[120px]"
          />
        </ElFormItem>
        <ElFormItem label="预览文档" class="!mb-0 min-w-[260px]">
          <ElSelect
            v-if="fileOptions.length"
            v-model="currentFileIndex"
            class="!w-full min-w-[240px]"
            placeholder="选择要预览的文档"
          >
            <ElOption
              v-for="item in fileOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="truncate">{{ item.label }}</span>
                <span v-if="item.segmentCount" class="text-xs text-gray-400">
                  {{ item.segmentCount }} 段
                </span>
              </div>
            </ElOption>
          </ElSelect>
          <span v-else class="text-sm text-gray-400">暂无文档</span>
        </ElFormItem>
      </ElForm>

      <ElButton type="primary" plain :loading="splitLoading" @click="handleRefreshPreview">
        重新预览
      </ElButton>

      <div v-if="segmentSummary" class="ml-auto text-sm text-gray-500">
        共 {{ segmentSummary.count }} 段 ·
        {{ segmentSummary.totalChars }} 字符 ·
        {{ segmentSummary.totalTokens }} Token
        <ElTooltip content="系统会按当前 Token 设置自动切分，此处仅供提交前确认">
          <IconifyIcon icon="lucide:circle-help" class="ml-1 inline text-gray-400" />
        </ElTooltip>
      </div>
    </div>

    <div class="flex h-[440px] overflow-hidden rounded-lg border border-border">
      <div class="flex w-[300px] shrink-0 flex-col border-r border-border bg-muted/10">
        <div class="border-b border-border px-3 py-2.5 text-sm font-medium">
          分片列表
        </div>
        <div class="min-h-0 flex-1 overflow-y-auto p-2">
          <div v-if="splitLoading" class="flex items-center justify-center py-10 text-sm text-gray-500">
            <IconifyIcon icon="lucide:loader" class="mr-2 animate-spin" />
            正在生成分段…
          </div>
          <ElEmpty
            v-else-if="!currentFile?.segments?.length"
            :image-size="64"
            description="点击「重新预览」生成分段"
          />
          <button
            v-for="(segment, index) in currentFile?.segments || []"
            :key="index"
            type="button"
            class="mb-2 w-full rounded-md border px-3 py-2 text-left transition-colors"
            :class="
              activeSegmentIndex === index
                ? 'border-primary bg-primary/5'
                : 'border-transparent bg-card hover:border-border'
            "
            @click="activeSegmentIndex = index as number"
          >
            <div class="mb-1 flex items-center justify-between text-xs">
              <span class="font-medium text-primary">分片 {{ (index as number) + 1 }}</span>
              <span class="text-gray-400">{{ segment.tokens || 0 }} tok</span>
            </div>
            <div class="line-clamp-2 text-xs leading-relaxed text-gray-500">
              {{ segmentPreview(segment.content) }}
            </div>
          </button>
        </div>
      </div>

      <div class="flex min-w-0 flex-1 flex-col">
        <div
          v-if="activeSegment"
          class="flex items-center justify-between border-b border-border px-4 py-2.5 text-sm"
        >
          <span class="font-medium">分片 {{ activeSegmentIndex + 1 }} 内容</span>
          <span class="text-xs text-gray-400">
            {{ activeSegment.contentLength || 0 }} 字符 ·
            {{ activeSegment.tokens || 0 }} Token
          </span>
        </div>
        <div class="min-h-0 flex-1 overflow-y-auto p-4">
          <pre
            v-if="activeSegment?.content"
            class="whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-foreground"
          >{{ activeSegment.content }}</pre>
          <ElEmpty v-else :image-size="80" description="从左侧选择分片查看内容" />
        </div>
      </div>
    </div>

    <div class="mt-5 flex justify-between">
      <ElButton v-if="!modelData.id" @click="handlePrevStep">上一步</ElButton>
      <div v-else />
      <ElButton type="primary" :loading="submitLoading" @click="handleSave">
        保存并向量化
      </ElButton>
    </div>
  </div>
</template>
