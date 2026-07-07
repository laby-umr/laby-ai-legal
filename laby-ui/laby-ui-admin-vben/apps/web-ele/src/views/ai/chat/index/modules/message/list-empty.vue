<!-- 消息列表为空时：全宽欢迎页 -->
<script setup lang="ts">
import type { PropType } from 'vue';

import type { AiChatConversationApi } from '#/api/ai/chat/conversation';

import { computed } from 'vue';

import { preferences } from '@vben/preferences';
import { IconifyIcon } from '@vben/icons';

import { LEGAL_ORCHESTRATION_ROLE_ID } from '#/api/legal/orchestration';

import './chat-welcome.scss';

const props = defineProps({
  conversation: {
    type: Object as PropType<AiChatConversationApi.ChatConversation | null>,
    default: null,
  },
  /** 上方已有角色设定卡片时使用更紧凑布局 */
  compact: {
    type: Boolean,
    default: false,
  },
});

const emits = defineEmits(['onPrompt']);

/** 暂时关闭示例问题入口，恢复时改为 true */
const showQuickPrompts = false;

const appTitle = computed(() => preferences.app.name || '智能助手');

const isLegalOrchestration = computed(
  () => props.conversation?.roleId === LEGAL_ORCHESTRATION_ROLE_ID,
);

const heroTitle = computed(() =>
  isLegalOrchestration.value ? '法务编排助手' : `${appTitle.value} · AI 对话`,
);

const heroDesc = computed(() =>
  isLegalOrchestration.value
    ? '上传合同附件、确认分类与审核提案，我可以协助你完成从登记文件到创建合同的全流程编排。'
    : '在下方选择示例问题，或直接在输入框描述你的任务。支持上下文记忆、附件上传与联网搜索。',
);

const promptList = computed(() => {
  if (isLegalOrchestration.value) {
    return [
      {
        icon: 'lucide:folder-up',
        label: '帮我登记刚上传的合同附件',
        hint: '自动识别对话中的最新文件',
        prompt: '请帮我登记对话里最新上传的合同附件，并说明下一步该怎么做。',
      },
      {
        icon: 'lucide:tags',
        label: '对文件进行合同类型分类',
        hint: '生成前端可确认的提案',
        prompt: '请对已登记文件进行合同类型分类，并生成分类确认提案。',
      },
      {
        icon: 'lucide:shield-alert',
        label: '预览审核主要风险点',
        hint: '先预览，再决定是否建合同',
        prompt: '请先对已分类文件执行预览审核，并总结高风险条款。',
      },
      {
        icon: 'lucide:file-signature',
        label: '生成创建合同提案',
        hint: '需在前端卡片确认执行',
        prompt: '分类完成后，请生成创建合同提案，并说明我还需要在前端确认哪些步骤。',
      },
    ];
  }
  return [
    {
      icon: 'lucide:file-search',
      label: '总结文档要点',
      hint: '适合长文、方案、报告',
      prompt: '请用三条要点总结这份文档的核心结论，并标注需要关注的风险。',
    },
    {
      icon: 'lucide:mail',
      label: '改写商务邮件',
      hint: '语气专业、结构清晰',
      prompt: '请把下面的内容改写成一封专业、简洁的商务邮件。',
    },
    {
      icon: 'lucide:list-checks',
      label: '制定行动计划',
      hint: '拆解任务与优先级',
      prompt: '根据我的目标，帮我列出本周可执行的行动计划，按优先级排序。',
    },
    {
      icon: 'lucide:sparkles',
      label: '头脑风暴创意',
      hint: '多角度给出方案',
      prompt: '针对这个主题，请从成本、效率、风险三个角度各给出一个可行方案。',
    },
  ];
});

const features = computed(() => {
  if (isLegalOrchestration.value) {
    return ['附件登记', '分类提案', '预览审核', '合同创建'];
  }
  return ['多轮上下文', '附件理解', '联网搜索', '流式回复'];
});

function handlerPromptClick(prompt: string) {
  emits('onPrompt', prompt);
}
</script>

<template>
  <div class="chat-welcome" :class="{ 'chat-welcome--compact': compact }">
    <div class="chat-welcome__backdrop" aria-hidden="true">
      <div class="chat-welcome__orb chat-welcome__orb--primary"></div>
      <div class="chat-welcome__orb chat-welcome__orb--soft"></div>
      <div class="chat-welcome__grid-line"></div>
    </div>

    <div class="chat-welcome__content">
      <section v-if="!compact" class="chat-welcome__hero">
        <span class="chat-welcome__badge">
          <IconifyIcon icon="lucide:sparkles" class="size-3.5" />
          新建对话
        </span>
        <h1 class="chat-welcome__title">{{ heroTitle }}</h1>
        <p class="chat-welcome__desc">{{ heroDesc }}</p>
        <div class="chat-welcome__features">
          <span
            v-for="item in features"
            :key="item"
            class="chat-welcome__feature"
          >
            <IconifyIcon icon="lucide:check" class="size-3 text-[var(--el-color-primary)]" />
            {{ item }}
          </span>
        </div>
        <p
          v-if="conversation?.modelName"
          class="chat-welcome__desc !mt-1 !text-xs"
        >
          当前模型：{{ conversation.modelName }}
        </p>
      </section>

      <section v-if="showQuickPrompts" class="chat-welcome__panel">
        <h2 class="chat-welcome__panel-title">
          {{ compact ? '试试这些问题' : '快速开始' }}
        </h2>
        <div class="chat-welcome__prompts">
          <button
            v-for="item in promptList"
            :key="item.prompt"
            type="button"
            class="chat-welcome__prompt"
            @click="handlerPromptClick(item.prompt)"
          >
            <span class="chat-welcome__prompt-icon">
              <IconifyIcon :icon="item.icon" class="size-4" />
            </span>
            <span class="chat-welcome__prompt-text">
              <span class="chat-welcome__prompt-label">{{ item.label }}</span>
              <span class="chat-welcome__prompt-hint">{{ item.hint }}</span>
            </span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
