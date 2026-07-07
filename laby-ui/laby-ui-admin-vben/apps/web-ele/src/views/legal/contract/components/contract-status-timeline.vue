<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed } from 'vue';

import { ElStep, ElSteps } from 'element-plus';

import {
  CONTRACT_STATUS,
  CONTRACT_STATUS_TIMELINE,
  formatContractTaskKeyLabel,
} from '../constants';

const props = defineProps<{
  contract?: LegalContractApi.Contract;
}>();

const activeIndex = computed(() => {
  const status = props.contract?.status;
  if (status === undefined || status === null) {
    return 0;
  }
  if (status === CONTRACT_STATUS.FAILED) {
    const idx = CONTRACT_STATUS_TIMELINE.findIndex(
      (item) => item.status === CONTRACT_STATUS.AI_AUDITING,
    );
    return idx >= 0 ? idx : 0;
  }
  if (status === CONTRACT_STATUS.REJECTED || status === CONTRACT_STATUS.CANCELLED) {
    return CONTRACT_STATUS_TIMELINE.length;
  }
  if (status === CONTRACT_STATUS.ARCHIVED) {
    // ElSteps 的 active 表示「当前进行中的步骤」；全部完成时需设为 length
    return CONTRACT_STATUS_TIMELINE.length;
  }
  const idx = CONTRACT_STATUS_TIMELINE.findIndex((item) => item.status === status);
  if (idx >= 0) {
    return idx;
  }
  return 0;
});

const processStatus = computed(() => {
  const status = props.contract?.status;
  if (status === CONTRACT_STATUS.FAILED) {
    return 'error';
  }
  if (
    status === CONTRACT_STATUS.REJECTED ||
    status === CONTRACT_STATUS.CANCELLED
  ) {
    return 'error';
  }
  if (status === CONTRACT_STATUS.ARCHIVED) {
    return 'success';
  }
  return 'process';
});
</script>

<template>
  <div
    v-if="contract"
    class="rounded-lg border border-border bg-muted/10 px-4 py-4"
  >
    <div class="mb-3 text-sm font-medium text-foreground">业务进度</div>
    <ElSteps
      :active="activeIndex"
      :finish-status="contract.status === CONTRACT_STATUS.ARCHIVED ? 'success' : 'finish'"
      :process-status="processStatus"
      align-center
    >
      <ElStep
        v-for="step in CONTRACT_STATUS_TIMELINE"
        :key="step.status"
        :title="step.title"
      />
    </ElSteps>
    <p
      v-if="contract.status === CONTRACT_STATUS.FAILED"
      class="mt-3 text-sm text-destructive"
    >
      处理失败：{{ contract.failReason || '请从列表重试' }}
    </p>
    <p
      v-else-if="contract.status === CONTRACT_STATUS.ARCHIVED"
      class="mt-3 text-xs text-muted-foreground"
    >
      流程已完成，合同已归档
    </p>
    <p
      v-else-if="contract.currentTaskKey"
      class="mt-3 text-xs text-muted-foreground"
    >
      当前流程节点：{{
        formatContractTaskKeyLabel(contract.currentTaskKey, contract.status)
      }}
    </p>
  </div>
</template>
