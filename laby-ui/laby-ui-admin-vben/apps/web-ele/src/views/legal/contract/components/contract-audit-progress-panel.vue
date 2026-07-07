<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed, onUnmounted, ref, watch } from 'vue';

import { ElAlert, ElProgress } from 'element-plus';

import { getAuditProgress } from '#/api/legal/contract';

const props = defineProps<{
  contractId: number;
  /** 合同处于 AI 审核中，或已提交二轮等待进度时由父组件传入 true */
  active: boolean;
  /** 审核轮次，用于文案（1=首轮，2=二轮） */
  auditRound?: number;
}>();

const emit = defineEmits<{
  completed: [];
}>();

const progress = ref<LegalContractApi.AuditProgress>();
let timer: ReturnType<typeof setInterval> | undefined;

const progressTitle = computed(() => {
  if (progress.value?.message) {
    return progress.value.message;
  }
  const roundLabel = (props.auditRound ?? progress.value?.auditRound ?? 1) >= 2
    ? '二轮'
    : '首轮';
  return `${roundLabel} AI 审核进行中，请稍候…`;
});

async function poll() {
  if (!props.contractId) {
    return;
  }
  try {
    progress.value = await getAuditProgress(props.contractId);
    if (
      progress.value?.status === 'COMPLETED' ||
      progress.value?.status === 'FAILED'
    ) {
      stopPoll();
      if (progress.value.status === 'COMPLETED') {
        emit('completed');
      }
    }
  } catch {
    /* 轮询失败不打断页面 */
  }
}
function startPoll() {
  stopPoll();
  void poll();
  timer = setInterval(() => void poll(), 2000);
}

function stopPoll() {
  if (timer) {
    clearInterval(timer);
    timer = undefined;
  }
}

watch(
  () => [props.active, props.contractId] as const,
  ([active]) => {
    if (active && props.contractId) {
      startPoll();
    } else {
      stopPoll();
      progress.value = undefined;
    }
  },
  { immediate: true },
);

onUnmounted(stopPoll);

const percent = () => {
  const p = progress.value;
  if (!p?.totalBatches || !p.batchIndex) {
    return undefined;
  }
  return Math.min(100, Math.round((p.batchIndex / p.totalBatches) * 100));
};
</script>

<template>
  <ElAlert
    v-if="active"
    type="info"
    :closable="false"
    show-icon
    class="mb-3"
  >
    <template #title>
      {{ progressTitle }}
      <span
        v-if="progress?.batchIndex && progress?.totalBatches"
        class="ml-1 text-xs font-normal opacity-80"
      >
        （批次 {{ progress.batchIndex }}/{{ progress.totalBatches }}）
      </span>
    </template>
    <ElProgress
      v-if="percent() !== undefined"
      :percentage="percent()!"
      :stroke-width="6"
      class="mt-2"
      :indeterminate="progress?.status === 'RUNNING' && percent() === 0"
    />
    <pre
      v-if="progress?.reasoningContent"
      class="mt-2 max-h-40 overflow-auto whitespace-pre-wrap rounded bg-muted/50 p-2 text-xs leading-relaxed"
    >{{ progress.reasoningContent }}</pre>
  </ElAlert>
</template>