<script lang="ts" setup>
import type { AiChatConversationApi } from '#/api/ai/chat/conversation';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { ElAvatar } from 'element-plus';

import {
  parseRolePrompt,
  tokenizeToolNames,
} from '../../utils/parse-role-prompt';

defineOptions({ name: 'AiChatRolePromptMessage' });

const props = defineProps<{
  content?: string;
  conversation?: AiChatConversationApi.ChatConversation | null;
}>();

const parsed = computed(() => parseRolePrompt(props.content?.trim() ?? ''));

const roleTitle = computed(
  () => props.conversation?.title?.trim() || '当前角色',
);

const contentLength = computed(() => props.content?.length ?? 0);

const sectionCount = computed(() => parsed.value.sections.length);

const isLongPrompt = computed(
  () => contentLength.value > 320 || sectionCount.value > 2,
);

const detailsExpanded = ref(false);

watch(
  isLongPrompt,
  (long) => {
    detailsExpanded.value = !long;
  },
  { immediate: true },
);

const introPreview = computed(() => {
  const intro = parsed.value.intro.trim();
  if (intro) {
    return intro;
  }
  const full = props.content?.trim() ?? '';
  if (full.length <= 140) {
    return full;
  }
  return `${full.slice(0, 140)}…`;
});

function toggleDetails() {
  detailsExpanded.value = !detailsExpanded.value;
}
</script>

<template>
  <article class="role-prompt" aria-label="角色设定">
    <div class="role-prompt__glow" aria-hidden="true"></div>

    <header class="role-prompt__header">
      <ElAvatar
        v-if="conversation?.roleAvatar"
        :src="conversation.roleAvatar"
        :size="40"
        class="role-prompt__avatar"
      />
      <div v-else class="role-prompt__avatar role-prompt__avatar--fallback">
        <IconifyIcon icon="lucide:bot" class="size-5" />
      </div>
      <div class="role-prompt__meta">
        <span class="role-prompt__eyebrow">
          <IconifyIcon icon="lucide:sparkles" class="size-3" />
          角色已启用
        </span>
        <h3 class="role-prompt__title">{{ roleTitle }}</h3>
        <p v-if="conversation?.modelName" class="role-prompt__model">
          模型 · {{ conversation.modelName }}
        </p>
      </div>
      <button
        v-if="isLongPrompt"
        type="button"
        class="role-prompt__header-toggle"
        :aria-expanded="detailsExpanded"
        @click="toggleDetails"
      >
        <IconifyIcon
          :icon="detailsExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'"
          class="size-4"
        />
      </button>
    </header>

    <div class="role-prompt__body">
      <p
        class="role-prompt__intro"
        :class="{ 'role-prompt__intro--clamp': isLongPrompt && !detailsExpanded }"
      >
        <template
          v-for="(token, idx) in tokenizeToolNames(introPreview)"
          :key="`intro-${idx}`"
        >
          <code v-if="token.type === 'tool'" class="role-prompt__tool">{{
            token.value
          }}</code>
          <span v-else>{{ token.value }}</span>
        </template>
      </p>

      <div
        v-if="isLongPrompt && !detailsExpanded"
        class="role-prompt__summary-bar"
      >
        <span v-if="sectionCount > 0" class="role-prompt__badge">
          {{ sectionCount }} 条工作指引
        </span>
        <button type="button" class="role-prompt__toggle" @click="toggleDetails">
          展开完整设定
          <IconifyIcon icon="lucide:chevron-down" class="size-3.5" />
        </button>
      </div>

      <div
        v-if="!isLongPrompt || detailsExpanded"
        class="role-prompt__details"
        :class="{ 'role-prompt__details--scroll': isLongPrompt }"
      >
        <ol v-if="sectionCount > 0" class="role-prompt__list">
          <li
            v-for="section in parsed.sections"
            :key="section.index"
            class="role-prompt__item"
          >
            <span class="role-prompt__index">{{ section.index }}</span>
            <p class="role-prompt__item-text">
              <template
                v-for="(token, idx) in tokenizeToolNames(section.body)"
                :key="`${section.index}-${idx}`"
              >
                <code v-if="token.type === 'tool'" class="role-prompt__tool">{{
                  token.value
                }}</code>
                <span v-else>{{ token.value }}</span>
              </template>
            </p>
          </li>
        </ol>

        <p
          v-else-if="detailsExpanded || !isLongPrompt"
          class="role-prompt__full"
        >
          <template
            v-for="(token, idx) in tokenizeToolNames(content ?? '')"
            :key="`full-${idx}`"
          >
            <code v-if="token.type === 'tool'" class="role-prompt__tool">{{
              token.value
            }}</code>
            <span v-else>{{ token.value }}</span>
          </template>
        </p>
      </div>

      <button
        v-if="isLongPrompt && detailsExpanded"
        type="button"
        class="role-prompt__collapse"
        @click="toggleDetails"
      >
        <IconifyIcon icon="lucide:chevron-up" class="size-3.5" />
        收起设定
      </button>
    </div>
  </article>
</template>

<style scoped>
.role-prompt {
  position: relative;
  flex-shrink: 0;
  margin: 12px 20px 0;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, var(--el-border-color-lighter));
  border-radius: 16px;
  background:
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--el-color-primary) 5%, var(--el-bg-color)) 0%,
      var(--el-bg-color) 55%
    );
  box-shadow:
    0 1px 2px color-mix(in srgb, var(--el-color-primary) 6%, transparent),
    0 8px 20px color-mix(in srgb, var(--el-color-primary) 4%, transparent);
}

.role-prompt__glow {
  position: absolute;
  top: -40px;
  right: -20px;
  width: 160px;
  height: 160px;
  pointer-events: none;
  background: radial-gradient(
    circle,
    color-mix(in srgb, var(--el-color-primary) 16%, transparent) 0%,
    transparent 70%
  );
}

.role-prompt__header {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 14px 16px 0;
}

.role-prompt__avatar {
  flex-shrink: 0;
  border: 2px solid var(--el-color-primary-light-8);
}

.role-prompt__avatar--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 50%;
}

.role-prompt__meta {
  flex: 1;
  min-width: 0;
}

.role-prompt__header-toggle {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  background: var(--el-fill-color-light);
  border: none;
  border-radius: 8px;
}

.role-prompt__header-toggle:hover {
  color: var(--el-color-primary);
}

.role-prompt__eyebrow {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  color: var(--el-color-primary);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.role-prompt__title {
  margin: 4px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.35;
  color: var(--el-text-color-primary);
  white-space: nowrap;
}

.role-prompt__model {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.role-prompt__body {
  padding: 12px 16px 14px;
}

.role-prompt__intro {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
}

.role-prompt__intro--clamp {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.role-prompt__summary-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.role-prompt__badge {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 999px;
}

.role-prompt__toggle {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-color-primary);
  cursor: pointer;
  background: transparent;
  border: none;
}

.role-prompt__toggle:hover {
  color: var(--el-color-primary-dark-2);
}

.role-prompt__details {
  margin-top: 10px;
}

.role-prompt__details--scroll {
  max-height: min(36vh, 280px);
  padding-right: 4px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.role-prompt__details--scroll::-webkit-scrollbar {
  width: 5px;
}

.role-prompt__details--scroll::-webkit-scrollbar-thumb {
  background: var(--el-border-color);
  border-radius: 999px;
}

.role-prompt__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.role-prompt__item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding: 8px 10px;
  background: color-mix(in srgb, var(--el-fill-color-light) 60%, transparent);
  border: 1px solid var(--el-border-color-extra-light);
  border-radius: 10px;
}

.role-prompt__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 11px;
  font-weight: 700;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 6px;
}

.role-prompt__item-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

.role-prompt__tool {
  padding: 0 4px;
  margin: 0 1px;
  font-family: ui-monospace, 'Cascadia Code', 'SF Mono', Consolas, monospace;
  font-size: 0.86em;
  color: var(--el-color-primary);
  word-break: break-all;
  background: color-mix(in srgb, var(--el-color-primary) 10%, var(--el-fill-color-blank));
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 4px;
}

.role-prompt__full {
  margin: 0;
  font-size: 12px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
}

.role-prompt__collapse {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 0;
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  background: transparent;
  border: none;
}

.role-prompt__collapse:hover {
  color: var(--el-color-primary);
}
</style>
