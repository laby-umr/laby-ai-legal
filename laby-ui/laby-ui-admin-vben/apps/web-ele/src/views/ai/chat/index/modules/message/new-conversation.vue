<script setup lang="ts">
import { computed } from 'vue';

import { preferences } from '@vben/preferences';
import { IconifyIcon } from '@vben/icons';

import { ElButton } from 'element-plus';

import './chat-welcome.scss';

const emits = defineEmits(['onNewConversation']);

const appTitle = computed(() => preferences.app.name || '智能助手');

function handlerNewChat() {
  emits('onNewConversation');
}
</script>

<template>
  <div class="chat-welcome">
    <div class="chat-welcome__backdrop" aria-hidden="true">
      <div class="chat-welcome__orb chat-welcome__orb--primary"></div>
      <div class="chat-welcome__orb chat-welcome__orb--soft"></div>
      <div class="chat-welcome__grid-line"></div>
    </div>

    <div class="chat-welcome__content">
      <section class="chat-welcome__hero">
        <span class="chat-welcome__badge">
          <IconifyIcon icon="lucide:message-square-plus" class="size-3.5" />
          开始协作
        </span>
        <h1 class="chat-welcome__title">{{ appTitle }} · AI 工作台</h1>
        <p class="chat-welcome__desc">
          从左侧选择历史对话，或新建一个会话。你可以切换角色与模型，处理问答、写作、法务编排等任务。
        </p>
        <div class="chat-welcome__features">
          <span class="chat-welcome__feature">
            <IconifyIcon icon="lucide:history" class="size-3 text-[var(--el-color-primary)]" />
            历史会话
          </span>
          <span class="chat-welcome__feature">
            <IconifyIcon icon="lucide:bot" class="size-3 text-[var(--el-color-primary)]" />
            多角色
          </span>
          <span class="chat-welcome__feature">
            <IconifyIcon icon="lucide:cpu" class="size-3 text-[var(--el-color-primary)]" />
            多模型
          </span>
        </div>
        <div class="chat-welcome__cta">
          <ElButton type="primary" size="large" round @click="handlerNewChat">
            <IconifyIcon icon="lucide:plus" class="mr-1.5" />
            新建对话
          </ElButton>
        </div>
      </section>

      <section class="chat-welcome__panel">
        <h2 class="chat-welcome__panel-title">你可以这样用</h2>
        <div class="chat-welcome__prompts">
          <div class="chat-welcome__prompt !cursor-default hover:!transform-none">
            <span class="chat-welcome__prompt-icon">
              <IconifyIcon icon="lucide:messages-square" class="size-4" />
            </span>
            <span class="chat-welcome__prompt-text">
              <span class="chat-welcome__prompt-label">日常问答与写作</span>
              <span class="chat-welcome__prompt-hint">总结、改写、翻译、头脑风暴</span>
            </span>
          </div>
          <div class="chat-welcome__prompt !cursor-default hover:!transform-none">
            <span class="chat-welcome__prompt-icon">
              <IconifyIcon icon="lucide:paperclip" class="size-4" />
            </span>
            <span class="chat-welcome__prompt-text">
              <span class="chat-welcome__prompt-label">附件与上下文</span>
              <span class="chat-welcome__prompt-hint">上传文件并结合多轮记忆分析</span>
            </span>
          </div>
          <div class="chat-welcome__prompt !cursor-default hover:!transform-none">
            <span class="chat-welcome__prompt-icon">
              <IconifyIcon icon="lucide:scale" class="size-4" />
            </span>
            <span class="chat-welcome__prompt-text">
              <span class="chat-welcome__prompt-label">法务编排流程</span>
              <span class="chat-welcome__prompt-hint">登记、分类、预览审核、创建合同</span>
            </span>
          </div>
          <div class="chat-welcome__prompt !cursor-default hover:!transform-none">
            <span class="chat-welcome__prompt-icon">
              <IconifyIcon icon="lucide:user-cog" class="size-4" />
            </span>
            <span class="chat-welcome__prompt-text">
              <span class="chat-welcome__prompt-label">角色仓库</span>
              <span class="chat-welcome__prompt-hint">左侧「新建对话」下方切换 Agent</span>
            </span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
