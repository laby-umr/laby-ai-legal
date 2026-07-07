<script lang="ts" setup>
import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

defineOptions({ name: 'AiChatSystemNoticeMessage' });

const props = defineProps<{
  content?: string;
}>();

const displayText = computed(() => {
  const raw = props.content?.trim() ?? '';
  return raw.replace(/^【系统通知】\s*/, '');
});
</script>

<template>
  <div class="system-notice" role="status">
    <div class="system-notice__icon" aria-hidden="true">
      <IconifyIcon icon="lucide:bell-ring" class="size-4" />
    </div>
    <div class="system-notice__body">
      <span class="system-notice__label">系统通知</span>
      <p class="system-notice__text">{{ displayText }}</p>
    </div>
  </div>
</template>

<style scoped>
.system-notice {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
  padding: 12px 14px;
  margin: 8px 0 16px;
  background: color-mix(in srgb, var(--el-color-primary) 6%, var(--el-fill-color-blank));
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 12px;
  box-shadow: 0 1px 2px color-mix(in srgb, var(--el-color-primary) 8%, transparent);
}

.system-notice__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
}

.system-notice__body {
  flex: 1;
  min-width: 0;
}

.system-notice__label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  letter-spacing: 0.02em;
}

.system-notice__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
  word-break: break-word;
}
</style>
