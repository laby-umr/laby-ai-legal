<script lang="ts" setup>
import { RouterLink } from 'vue-router';

import { LEGAL_CONTRACT_CHAT_ROLE_CATEGORY } from '#/api/ai/model/chatRole';

/** CFG-001：规则与条款配置顺序引导 */
const steps = [
  {
    order: 1,
    title: '标准条款库',
    desc: '维护公司标准写法范本',
    to: '/legal/rule/standard-clause',
  },
  {
    order: 2,
    title: '审核规则',
    desc: '引用标准条款，定义检查逻辑',
    to: '/legal/rule/audit-rule',
  },
  {
    order: 3,
    title: '聊天角色',
    desc: '编写审核/问答提示词（法务合同分类）',
    to: {
      name: 'LegalContractPromptSettings',
      query: { category: LEGAL_CONTRACT_CHAT_ROLE_CATEGORY },
    },
  },
  {
    order: 4,
    title: 'AI 技能包',
    desc: '按场景绑定角色 + Agent 工具',
    to: '/legal/rule/skill-pack',
  },
  {
    order: 5,
    title: '合同类型',
    desc: '绑定知识库与默认技能包',
    to: '/legal/rule/contract-type',
  },
];
</script>

<template>
  <div
    class="legal-config-banner mb-3 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3"
  >
    <div class="mb-2 text-sm font-medium text-foreground">
      法务 AI 配置顺序（避免重复配置）
    </div>
    <ol class="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
      <li v-for="step in steps" :key="step.order" class="flex items-center gap-1">
        <span
          class="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[10px] font-medium text-primary"
        >{{ step.order }}</span>
        <RouterLink
          :to="step.to"
          class="text-foreground underline-offset-2 hover:text-primary hover:underline"
        >
          {{ step.title }}
        </RouterLink>
        <span>— {{ step.desc }}</span>
      </li>
    </ol>
  </div>
</template>
