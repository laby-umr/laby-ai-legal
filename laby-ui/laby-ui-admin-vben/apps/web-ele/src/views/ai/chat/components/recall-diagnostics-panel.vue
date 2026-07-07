<script setup lang="ts">
import type { AiChatMessageApi } from '#/api/ai/chat/message';

import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { ElTag } from 'element-plus';

const props = withDefaults(
  defineProps<{
    alwaysExpanded?: boolean;
    recallDiagnostics?: AiChatMessageApi.ChatRecallDiagnostics;
  }>(),
  {
    alwaysExpanded: false,
  },
);

const isExpanded = ref(props.alwaysExpanded);

const hasDiagnostics = computed(() => {
  const d = props.recallDiagnostics;
  if (!d) {
    return false;
  }
  return (
    d.totalHitCount != null ||
    d.noAnswerGuard != null ||
    (d.items?.length ?? 0) > 0
  );
});

const summaryText = computed(() => {
  const d = props.recallDiagnostics;
  if (!d) {
    return '';
  }
  const parts: string[] = [];
  if (d.totalHitCount != null) {
    parts.push(`命中 ${d.totalHitCount} 段`);
  }
  if (d.items?.length) {
    parts.push(`${d.items.length} 个知识库`);
  }
  const latency = d.items?.find((item) => item.latencyMs != null)?.latencyMs;
  if (latency != null) {
    parts.push(`${latency}ms`);
  }
  return parts.join(' · ');
});

function toggleExpanded() {
  if (props.alwaysExpanded) {
    return;
  }
  isExpanded.value = !isExpanded.value;
}

function formatHitStats(item: AiChatMessageApi.ChatRecallDiagnosticsItem) {
  const parts: string[] = [];
  if (item.denseHitCount != null) {
    parts.push(`Dense ${item.denseHitCount}`);
  }
  if (item.sparseHitCount != null) {
    parts.push(`Sparse ${item.sparseHitCount}`);
  }
  if (item.fusedHitCount != null) {
    parts.push(`融合 ${item.fusedHitCount}`);
  }
  if (item.rerankHitCount != null) {
    parts.push(`Rerank ${item.rerankHitCount}`);
  }
  if (item.topScore != null) {
    parts.push(`Top1 ${item.topScore}`);
  }
  if (item.latencyMs != null) {
    parts.push(`${item.latencyMs}ms`);
  }
  return parts.join(' · ');
}
</script>

<template>
  <div v-if="hasDiagnostics" :class="alwaysExpanded ? '' : 'mt-2.5'">
    <div
      v-if="!alwaysExpanded"
      class="mb-2 flex cursor-pointer items-center justify-between text-sm text-gray-600 transition-colors hover:text-blue-500"
      @click="toggleExpanded"
    >
      <div class="flex flex-wrap items-center gap-1.5">
        <IconifyIcon icon="lucide:activity" :size="14" />
        <span>召回诊断</span>
        <span v-if="summaryText" class="text-xs text-gray-400">
          {{ summaryText }}
        </span>
        <ElTag
          v-if="recallDiagnostics?.noAnswerGuard"
          size="small"
          type="warning"
        >
          无引用守卫
        </ElTag>
      </div>
      <IconifyIcon
        :icon="isExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'"
        class="text-xs transition-transform duration-200"
        :size="12"
      />
    </div>

    <div
      v-if="alwaysExpanded"
      class="mb-3 flex flex-wrap items-center gap-2 text-sm text-gray-600"
    >
      <span class="font-medium">召回诊断</span>
      <span v-if="summaryText" class="text-xs text-gray-400">
        {{ summaryText }}
      </span>
      <ElTag
        v-if="recallDiagnostics?.noAnswerGuard"
        size="small"
        type="warning"
      >
        无引用守卫
      </ElTag>
    </div>

    <div
      v-show="alwaysExpanded || isExpanded"
      class="flex flex-col gap-2 rounded-lg bg-gray-50 p-2.5 text-sm transition-all duration-200 ease-in-out"
    >
      <div
        v-for="(item, index) in recallDiagnostics?.items"
        :key="index"
        class="rounded-md border border-solid border-gray-200 bg-white p-2.5"
      >
        <div class="mb-1.5 flex flex-wrap items-center gap-2">
          <span class="font-medium text-gray-700">
            知识库 #{{ item.knowledgeId ?? index + 1 }}
          </span>
          <ElTag v-if="item.intent" size="small" type="info">
            {{ item.intent }}
          </ElTag>
        </div>
        <div
          v-if="item.queryVariants?.length"
          class="mb-1.5 flex flex-wrap gap-1"
        >
          <ElTag
            v-for="(variant, vIdx) in item.queryVariants"
            :key="vIdx"
            size="small"
            class="!max-w-full truncate"
          >
            {{ variant }}
          </ElTag>
        </div>
        <div class="text-xs text-gray-500">
          {{ formatHitStats(item) }}
        </div>
        <div
          v-if="item.notes?.length"
          class="mt-1.5 space-y-1 text-xs text-amber-700"
        >
          <div v-for="(note, nIdx) in item.notes" :key="nIdx">
            {{ note }}
          </div>
        </div>
      </div>

      <div
        v-if="!(recallDiagnostics?.items?.length)"
        class="text-xs text-gray-500"
      >
        <span v-if="recallDiagnostics?.totalHitCount != null">
          总命中 {{ recallDiagnostics.totalHitCount }} 段
        </span>
        <span v-else>暂无详细诊断项</span>
      </div>
    </div>
  </div>
</template>
