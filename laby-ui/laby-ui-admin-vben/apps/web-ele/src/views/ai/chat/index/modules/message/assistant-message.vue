<script lang="ts" setup>
import { computed, nextTick, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { useClipboard } from '@vueuse/core';
import { ElMessage } from 'element-plus';

import { decodeCodeFromAttr } from '#/utils/markdown';
import { renderStreamingMarkdown } from '#/utils/streaming-markdown';

import '#/styles/ai-markdown.scss';

defineOptions({ name: 'AiChatAssistantMessage' });

const props = defineProps<{
  content?: string;
  isLast?: boolean;
  reasoningContent?: string;
  showThinking?: boolean;
  streaming?: boolean;
  thinkingStreaming?: boolean;
}>();

const { copy } = useClipboard({ legacy: true });
const thinkingExpanded = ref(false);
const thinkingPanelRef = ref<HTMLElement>();

const displayThinking = computed(() => props.reasoningContent?.trim() ?? '');

const realContent = computed(() => {
  const text = props.content?.trim() ?? '';
  return text === '思考中...' ? '' : text;
});

const hasThinkingBlock = computed(
  () =>
    props.showThinking ||
    !!displayThinking.value ||
    props.thinkingStreaming === true,
);

const isThinkingOpen = computed(
  () => thinkingExpanded.value || props.thinkingStreaming === true,
);

const renderedAnswer = computed(() =>
  renderStreamingMarkdown(realContent.value, !!props.streaming),
);

async function scrollThinkingPanel() {
  await nextTick();
  const panel = thinkingPanelRef.value;
  if (panel) {
    panel.scrollTop = panel.scrollHeight;
  }
}

defineExpose({ scrollThinkingPanel });

function toggleThinking() {
  thinkingExpanded.value = !thinkingExpanded.value;
}

function handleAnswerClick(e: MouseEvent) {
  const btn = (e.target as HTMLElement).closest('.code-copy-btn') as
    | HTMLElement
    | null;
  if (!btn?.dataset.code) {
    return;
  }
  copy(decodeCodeFromAttr(btn.dataset.code));
  ElMessage.success('复制成功');
}

watch(
  () => props.thinkingStreaming,
  (v) => {
    if (v) {
      thinkingExpanded.value = true;
    }
  },
);

watch(
  () => displayThinking.value,
  (v) => {
    if (v && !props.thinkingStreaming) {
      thinkingExpanded.value = true;
    }
  },
  { immediate: true },
);

watch(
  () => displayThinking.value.length,
  () => {
    if (props.isLast && props.thinkingStreaming) {
      scrollThinkingPanel();
    }
  },
);
</script>

<template>
  <div class="assistant-message">
    <div class="ai-avatar" aria-hidden="true">
      <IconifyIcon icon="ep:chat-dot-round" class="size-[18px]" />
    </div>
    <div class="assistant-body">
      <div v-if="hasThinkingBlock" class="thinking-wrap">
        <button
          type="button"
          class="thinking-toggle"
          :class="{ streaming: thinkingStreaming }"
          :disabled="thinkingStreaming"
          @click="!thinkingStreaming && toggleThinking()"
        >
          <svg
            class="chevron"
            :class="{ open: isThinkingOpen }"
            width="10"
            height="10"
            viewBox="0 0 10 10"
            fill="none"
            aria-hidden="true"
          >
            <path
              d="M3.5 1.5L6.5 5L3.5 8.5"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span v-if="thinkingStreaming" class="toggle-text">Thinking</span>
          <span v-else class="toggle-text">Thought</span>
        </button>
        <div
          v-show="isThinkingOpen"
          ref="thinkingPanelRef"
          class="thinking-panel"
        >
          <span class="thinking-pre">{{ displayThinking }}</span>
          <span v-if="thinkingStreaming" class="caret"></span>
        </div>
      </div>
      <div
        v-if="realContent || (streaming && !thinkingStreaming)"
        class="markdown-body answer-md"
        v-dompurify-html="renderedAnswer"
        @click="handleAnswerClick"
      ></div>
      <span
        v-if="streaming && realContent && !thinkingStreaming"
        class="caret stream-caret"
      ></span>
    </div>
  </div>
</template>

<style scoped>
.assistant-message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding-bottom: 24px;
  font-size: 15px;
  line-height: 1.65;
  color: var(--el-text-color-primary);
}

.ai-avatar {
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

.assistant-body {
  flex: 1;
  min-width: 0;
}

.thinking-wrap {
  margin-bottom: 14px;
}

.thinking-toggle {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  padding: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  background: transparent;
  border: none;
}

.thinking-toggle:disabled {
  cursor: default;
}

.thinking-toggle:not(:disabled):hover .toggle-text {
  color: var(--el-text-color-regular);
}

.chevron {
  transition: transform 0.18s ease;
}

.chevron.open {
  transform: rotate(90deg);
}

.thinking-panel {
  max-height: min(36vh, 260px);
  margin-top: 6px;
  overflow: auto;
}

.thinking-pre {
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-placeholder);
  white-space: pre-wrap;
}

.caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 1px;
  vertical-align: text-bottom;
  background: var(--el-text-color-placeholder);
  animation: blink 1s step-end infinite;
}

.stream-caret {
  margin-top: 4px;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
