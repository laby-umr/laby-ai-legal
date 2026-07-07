<script lang="ts" setup>
import { IconifyIcon } from '@vben/icons';

defineOptions({ name: 'AiChatUserMessage' });

defineProps<{
  content?: string;
}>();
</script>

<template>
  <div class="user-message">
    <div v-if="content && content.trim()" class="user-bubble">
      {{ content }}
    </div>
    <div class="user-avatar" aria-hidden="true">
      <IconifyIcon icon="ep:user" class="size-[18px]" />
    </div>
  </div>
</template>

<style scoped>
.user-message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: flex-end;
  width: fit-content;
  max-width: min(88%, 44rem);
  margin-left: auto;
  padding: 4px 0 8px;
}

.user-avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 50%;
}

.user-bubble {
  flex: 1;
  min-width: 0;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-word;
  background: color-mix(in srgb, var(--el-color-primary) 8%, var(--el-fill-color-blank));
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 14px 14px 4px 14px;
}
</style>
