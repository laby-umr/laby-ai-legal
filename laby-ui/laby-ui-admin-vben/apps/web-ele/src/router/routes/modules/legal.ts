import type { RouteRecordRaw } from 'vue-router';

/** 法务合同隐藏路由（扁平完整 path，避免 name 导航 No match） */
const routes: RouteRecordRaw[] = [
  {
    path: '/legal/contract/list',
    name: 'LegalContractList',
    component: () => import('#/views/legal/contract/index.vue'),
    meta: {
      title: '我的合同',
      activePath: '/legal/contract',
      hideInMenu: true,
    },
  },
  {
    path: '/legal/contract/review',
    name: 'LegalContractReview',
    component: () => import('#/views/legal/contract/review.vue'),
    meta: {
      title: '审核工作台',
      activePath: '/legal/contract',
      hideInMenu: true,
    },
  },
  /** 与 review 同一页面，路由名用于标题区分；默认允许交互 */
  {
    path: '/legal/contract/detail',
    name: 'LegalContractDetail',
    component: () => import('#/views/legal/contract/review.vue'),
    meta: {
      title: '合同详情',
      activePath: '/legal/contract',
      hideInMenu: true,
    },
  },
  /** 复用 AI 聊天角色页，供「新建合同」管理提示词（不依赖 AI 菜单是否分配） */
  {
    path: '/legal/contract-memory',
    name: 'LegalContractMemory',
    component: () => import('#/views/legal/contract-memory/index.vue'),
    meta: {
      title: '情节记忆',
      activePath: '/legal/contract-memory',
      hideInMenu: true,
    },
  },
  {
    path: '/legal/contract/prompt-settings',
    name: 'LegalContractPromptSettings',
    component: () => import('#/views/ai/model/chatRole/index.vue'),
    meta: {
      title: '审核提示词',
      activePath: '/legal/contract',
      hideInMenu: true,
    },
  },
];

export default routes;
