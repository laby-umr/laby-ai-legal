<script setup lang="ts">
import type { PropType } from 'vue';

import type { AiChatConversationApi } from '#/api/ai/chat/conversation';
import type { AiChatMessageApi } from '#/api/ai/chat/message';

import { nextTick, onMounted, ref, toRefs } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { useClipboard } from '@vueuse/core';
import { ElButton, ElMessage, ElTag } from 'element-plus';

import { deleteChatMessage } from '#/api/ai/chat/message';

import AssistantMessage from './assistant-message.vue';
import CompactionSummaryMessage from './compaction-summary-message.vue';
import SystemNoticeMessage from './system-notice-message.vue';
import UserMessage from './user-message.vue';
import MessageFiles from './files.vue';
import MessageKnowledge from './knowledge.vue';
import MessageRecallDiagnostics from '#/views/ai/chat/components/recall-diagnostics-panel.vue';
import MessageWebSearch from './web-search.vue';

const props = defineProps({
  conversation: {
    type: Object as PropType<AiChatConversationApi.ChatConversation>,
    required: true,
  },
  list: {
    type: Array as PropType<AiChatMessageApi.ChatMessage[]>,
    required: true,
  },
  conversationInProgress: {
    type: Boolean,
    default: false,
  },
  roleHasKnowledge: {
    type: Boolean,
    default: false,
  },
});

const emits = defineEmits(['onDeleteSuccess', 'onRefresh', 'onEdit']);
const { copy } = useClipboard({ legacy: true });

const messageContainer: any = ref(null);
const isScrolling = ref(false);

const { list } = toRefs(props);

function isAssistantStreaming(index: number): boolean {
  if (!props.conversationInProgress) {
    return false;
  }
  const item = props.list[index];
  if (!item || item.type === 'user') {
    return false;
  }
  return index === props.list.length - 1;
}

function isThinkingStreaming(
  item: AiChatMessageApi.ChatMessage,
  index: number,
): boolean {
  if (!isAssistantStreaming(index)) {
    return false;
  }
  const content = item.content?.trim() || '';
  const hasRealContent = content && content !== '思考中...';
  return !hasRealContent;
}

function hasThinkingBlock(
  item: AiChatMessageApi.ChatMessage,
  index: number,
): boolean {
  return (
    !!item.reasoningContent?.trim() || isThinkingStreaming(item, index)
  );
}

function isSystemNotice(item: AiChatMessageApi.ChatMessage): boolean {
  const content = item.content?.trim() ?? '';
  return item.type === 'user' && content.startsWith('【系统通知】');
}

function isKnowledgeMiss(item: AiChatMessageApi.ChatMessage): boolean {
  if (!props.roleHasKnowledge || item.type === 'user' || item.type === 'summary') {
    return false;
  }
  if (item.recallDiagnostics?.noAnswerGuard === true) {
    return true;
  }
  const hasSegments =
    (item.segments?.length ?? 0) > 0 || (item.segmentIds?.length ?? 0) > 0;
  return !hasSegments;
}

async function scrollToBottom(isIgnore?: boolean) {
  await nextTick();
  if (isIgnore || !isScrolling.value) {
    messageContainer.value.scrollTop =
      messageContainer.value.scrollHeight - messageContainer.value.offsetHeight;
  }
}

function handleScroll() {
  const scrollContainer = messageContainer.value;
  const scrollTop = scrollContainer.scrollTop;
  const scrollHeight = scrollContainer.scrollHeight;
  const offsetHeight = scrollContainer.offsetHeight;
  isScrolling.value = scrollTop + offsetHeight < scrollHeight - 100;
}

async function handleGoBottom() {
  const scrollContainer = messageContainer.value;
  scrollContainer.scrollTop = scrollContainer.scrollHeight;
}

async function handlerGoTop() {
  const scrollContainer = messageContainer.value;
  scrollContainer.scrollTop = 0;
}

defineExpose({ scrollToBottom, handlerGoTop });

async function copyContent(content: string) {
  await copy(content);
  ElMessage.success('复制成功！');
}

async function handleDelete(id: number) {
  await deleteChatMessage(id);
  ElMessage.success('删除成功！');
  emits('onDeleteSuccess');
}

async function handleRefresh(message: AiChatMessageApi.ChatMessage) {
  emits('onRefresh', message);
}

async function handleEdit(message: AiChatMessageApi.ChatMessage) {
  emits('onEdit', message);
}

onMounted(async () => {
  messageContainer.value.addEventListener('scroll', handleScroll);
});
</script>
<template>
  <div ref="messageContainer" class="relative h-full overflow-y-auto">
    <div
      v-for="(item, index) in list"
      :key="index"
      class="message-group flex flex-col overflow-y-hidden px-5"
    >
      <!-- Compaction 摘要（Harness 上下文压缩落库） -->
      <template v-if="item.type === 'summary'">
        <CompactionSummaryMessage :content="item.content" />
        <div v-if="item.id > 0" class="assistant-extra ml-11">
          <ElButton
            class="!ml-0 flex items-center bg-transparent !px-1.5 hover:bg-gray-100"
            text
            @click="handleDelete(item.id)"
          >
            <IconifyIcon icon="lucide:trash" />
          </ElButton>
        </div>
      </template>

      <!-- 左侧消息：system、assistant -->
      <template v-else-if="item.type !== 'user'">
        <AssistantMessage
          :reasoning-content="item.reasoningContent || ''"
          :content="item.content || ''"
          :streaming="isAssistantStreaming(index)"
          :thinking-streaming="isThinkingStreaming(item, index)"
          :show-thinking="hasThinkingBlock(item, index)"
          :is-last="index === list.length - 1"
        />
        <div class="assistant-extra ml-11">
          <ElTag
            v-if="isKnowledgeMiss(item)"
            size="small"
            type="warning"
            class="mb-1"
          >
            未命中知识库
          </ElTag>
          <MessageFiles :attachment-urls="item.attachmentUrls" />
          <MessageKnowledge v-if="item.segments" :segments="item.segments" />
          <MessageRecallDiagnostics
            v-if="roleHasKnowledge && item.recallDiagnostics"
            :recall-diagnostics="item.recallDiagnostics"
          />
          <MessageWebSearch
            v-if="item.webSearchPages"
            :web-search-pages="item.webSearchPages"
          />
          <div class="mt-2 flex flex-row">
            <ElButton
              class="!ml-0 flex items-center bg-transparent !px-1.5 hover:bg-gray-100"
              text
              @click="copyContent(item.content)"
            >
              <IconifyIcon icon="lucide:copy" />
            </ElButton>
            <ElButton
              v-if="item.id > 0"
              class="!ml-1 flex items-center bg-transparent !px-1.5 hover:bg-gray-100"
              text
              @click="handleDelete(item.id)"
            >
              <IconifyIcon icon="lucide:trash" />
            </ElButton>
          </div>
        </div>
      </template>

      <!-- 系统通知（编排确认等） -->
      <template v-else-if="isSystemNotice(item)">
        <SystemNoticeMessage :content="item.content" />
      </template>

      <!-- 右侧消息：user -->
      <template v-else>
        <div class="user-message-group">
          <UserMessage :content="item.content" />
          <MessageFiles
            v-if="item.attachmentUrls && item.attachmentUrls.length > 0"
            class="user-message-group__files"
            :attachment-urls="item.attachmentUrls"
          />
          <div class="user-actions">
            <ElButton
              class="message-action-btn !ml-0"
              text
              @click="copyContent(item.content)"
            >
              <IconifyIcon icon="lucide:copy" />
            </ElButton>
            <ElButton
              class="message-action-btn !ml-1"
              text
              @click="handleDelete(item.id)"
            >
              <IconifyIcon icon="lucide:trash" />
            </ElButton>
            <ElButton
              class="message-action-btn !ml-1"
              text
              @click="handleRefresh(item)"
            >
              <IconifyIcon icon="lucide:refresh-cw" />
            </ElButton>
            <ElButton
              class="message-action-btn !ml-1"
              text
              @click="handleEdit(item)"
            >
              <IconifyIcon icon="lucide:edit" />
            </ElButton>
          </div>
        </div>
      </template>
    </div>
  </div>

  <div
    v-if="isScrolling"
    class="absolute bottom-0 right-1/2 z-1000"
    @click="handleGoBottom"
  >
    <ElButton circle>
      <IconifyIcon icon="lucide:chevron-down" />
    </ElButton>
  </div>
</template>

<style scoped>
.assistant-extra {
  margin-top: -4px;
}

.user-message-group {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  width: 100%;
  margin-bottom: 12px;
}

.user-message-group__files {
  width: fit-content;
  max-width: min(88%, 44rem);
  margin-top: 4px;
}

.user-actions {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  margin-top: 2px;
}

.message-action-btn {
  display: inline-flex;
  align-items: center;
  padding: 0 6px !important;
  color: var(--el-text-color-secondary);
  background: transparent !important;
}

.message-action-btn:hover {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light) !important;
}
</style>
