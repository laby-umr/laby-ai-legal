<script lang="ts" setup>
import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

defineOptions({ name: 'AiChatCompactionSummaryMessage' });

const props = defineProps<{
  content?: string;
}>();

function normalizeSummaryContent(raw: string): string {
  const markers = [
    'A condensed summary follows:',
    'Here is a summary of the conversation to date:',
  ];
  for (const marker of markers) {
    const index = raw.indexOf(marker);
    if (index >= 0) {
      return raw.substring(index + marker.length).trim();
    }
  }
  return raw.trim();
}

const displayText = computed(() =>
  normalizeSummaryContent(props.content?.trim() ?? ''),
);
</script>

<template>
  <div class="compaction-summary" role="note">
    <div class="compaction-summary__icon" aria-hidden="true">
      <IconifyIcon icon="lucide:archive" class="size-4" />
    </div>
    <div class="compaction-summary__body">
      <span class="compaction-summary__label">对话摘要（上下文压缩）</span>
      <p class="compaction-summary__text">{{ displayText }}</p>
    </div>
  </div>
</template>

<style scoped>
.compaction-summary {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
  padding: 12px 14px;
  margin: 8px 0 16px;
  background: color-mix(in srgb, var(--el-color-info) 6%, var(--el-fill-color-blank));
  border: 1px dashed var(--el-color-info-light-5);
  border-radius: 12px;
}

.compaction-summary__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--el-color-info);
  background: var(--el-color-info-light-9);
  border-radius: 8px;
}

.compaction-summary__body {
  flex: 1;
  min-width: 0;
}

.compaction-summary__label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-info);
  letter-spacing: 0.02em;
}

.compaction-summary__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--el-text-color-secondary);
  word-break: break-word;
  white-space: pre-wrap;
}
</style>
