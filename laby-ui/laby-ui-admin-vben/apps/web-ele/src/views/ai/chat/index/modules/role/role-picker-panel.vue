<script setup lang="ts">
import type { AiChatConversationApi } from '#/api/ai/chat/conversation';
import type { AiModelChatRoleApi } from '#/api/ai/model/chatRole';

import { onMounted, reactive, ref } from 'vue';

import { confirm, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  ElAvatar,
  ElButton,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElInput,
} from 'element-plus';

import { createChatConversationMy } from '#/api/ai/chat/conversation';
import { deleteMy, getCategoryList, getMyPage } from '#/api/ai/model/chatRole';

import Form from '../../../../model/chatRole/modules/form.vue';

const emit = defineEmits<{
  back: [];
  roleUsed: [conversationId: number];
}>();

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const loading = ref(false);
const usingRoleId = ref<number | null>(null);
const activeTab = ref<'my-role' | 'public-role'>('public-role');
const search = ref('');
const myRoleParams = reactive({ pageNo: 1, pageSize: 50 });
const publicRoleParams = reactive({ pageNo: 1, pageSize: 50 });
const myRoleList = ref<AiModelChatRoleApi.ChatRole[]>([]);
const publicRoleList = ref<AiModelChatRoleApi.ChatRole[]>([]);
const activeCategory = ref('全部');
const categoryList = ref<string[]>([]);

const activeRoleList = ref<AiModelChatRoleApi.ChatRole[]>([]);

async function getMyRole(append?: boolean) {
  const { list } = await getMyPage({
    ...myRoleParams,
    name: search.value,
    publicStatus: false,
  });
  myRoleList.value = append ? [...myRoleList.value, ...list] : list;
  if (activeTab.value === 'my-role') {
    activeRoleList.value = myRoleList.value;
  }
}

async function getPublicRole(append?: boolean) {
  const { list } = await getMyPage({
    ...publicRoleParams,
    category: activeCategory.value === '全部' ? '' : activeCategory.value,
    name: search.value,
    publicStatus: true,
  });
  publicRoleList.value = append ? [...publicRoleList.value, ...list] : list;
  if (activeTab.value === 'public-role') {
    activeRoleList.value = publicRoleList.value;
  }
}

async function reloadRoles() {
  loading.value = true;
  try {
    if (activeTab.value === 'my-role') {
      myRoleParams.pageNo = 1;
      await getMyRole();
    } else {
      publicRoleParams.pageNo = 1;
      await getPublicRole();
    }
  } finally {
    loading.value = false;
  }
}

async function switchTab(tab: 'my-role' | 'public-role') {
  activeTab.value = tab;
  await reloadRoles();
}

async function handlerCategoryClick(category: string) {
  activeCategory.value = category;
  await reloadRoles();
}

function handlerAddRole() {
  formModalApi.setData({ formType: 'my-create' }).open();
}

function handlerCardEdit(role: AiModelChatRoleApi.ChatRole) {
  formModalApi.setData({ formType: 'my-update', id: role.id }).open();
}

async function handlerCardDelete(role: AiModelChatRoleApi.ChatRole) {
  await confirm(`确认删除角色「${role.name}」？`);
  await deleteMy(role.id);
  await reloadRoles();
}

async function handlerCardUse(role: AiModelChatRoleApi.ChatRole) {
  if (usingRoleId.value) {
    return;
  }
  usingRoleId.value = role.id;
  try {
    const data = { roleId: role.id } as AiChatConversationApi.ChatConversation;
    const conversationId = await createChatConversationMy(data);
    emit('roleUsed', conversationId);
  } finally {
    usingRoleId.value = null;
  }
}

async function loadMore() {
  if (loading.value) {
    return;
  }
  loading.value = true;
  try {
    if (activeTab.value === 'public-role') {
      publicRoleParams.pageNo++;
      await getPublicRole(true);
    } else {
      myRoleParams.pageNo++;
      await getMyRole(true);
    }
  } finally {
    loading.value = false;
  }
}

function handleScroll(event: Event) {
  const target = event.target as HTMLElement;
  if (!target) {
    return;
  }
  const nearBottom =
    target.scrollTop + target.clientHeight >= target.scrollHeight - 24;
  if (nearBottom && !loading.value) {
    void loadMore();
  }
}

onMounted(async () => {
  categoryList.value = ['全部', ...(await getCategoryList())];
  await reloadRoles();
});
</script>

<template>
  <div class="role-picker">
    <FormModal @success="reloadRoles" />

    <header class="role-picker__header">
      <button type="button" class="role-picker__back" @click="emit('back')">
        <IconifyIcon icon="lucide:arrow-left" class="size-4" />
        返回
      </button>
      <h2 class="role-picker__title">角色仓库</h2>
      <p class="role-picker__subtitle">选择 Agent 开始新对话</p>
    </header>

    <div class="role-picker__toolbar">
      <ElInput
        v-model="search"
        size="small"
        clearable
        placeholder="搜索角色"
        @keyup.enter="reloadRoles"
        @clear="reloadRoles"
      >
        <template #prefix>
          <IconifyIcon icon="lucide:search" class="size-3.5" />
        </template>
      </ElInput>

      <div class="role-picker__tabs">
        <button
          type="button"
          class="role-picker__tab"
          :class="{ 'is-active': activeTab === 'public-role' }"
          @click="switchTab('public-role')"
        >
          公共角色
        </button>
        <button
          type="button"
          class="role-picker__tab"
          :class="{ 'is-active': activeTab === 'my-role' }"
          @click="switchTab('my-role')"
        >
          我的角色
        </button>
      </div>

      <div
        v-if="activeTab === 'public-role' && categoryList.length > 1"
        class="role-picker__categories"
      >
        <button
          v-for="category in categoryList"
          :key="category"
          type="button"
          class="role-picker__category"
          :class="{ 'is-active': category === activeCategory }"
          @click="handlerCategoryClick(category)"
        >
          {{ category }}
        </button>
      </div>

      <ElButton
        v-if="activeTab === 'my-role'"
        type="primary"
        plain
        size="small"
        class="role-picker__add-btn"
        @click="handlerAddRole"
      >
        <IconifyIcon icon="lucide:plus" class="mr-1" />
        添加角色
      </ElButton>
    </div>

    <div class="role-picker__list" @scroll="handleScroll">
      <ElEmpty
        v-if="!loading && activeRoleList.length === 0"
        description="暂无匹配角色"
        :image-size="72"
      />

      <button
        v-for="role in activeRoleList"
        :key="role.id"
        type="button"
        class="role-picker__item"
        :disabled="usingRoleId === role.id"
        @click="handlerCardUse(role)"
      >
        <ElAvatar :src="role.avatar" :size="36" class="role-picker__avatar" />
        <div class="role-picker__item-body">
          <div class="role-picker__item-head">
            <span class="role-picker__item-name">{{ role.name }}</span>
            <span v-if="role.category" class="role-picker__item-tag">{{
              role.category
            }}</span>
          </div>
          <p class="role-picker__item-desc">
            {{ role.description || '暂无描述' }}
          </p>
        </div>
        <div class="role-picker__item-actions" @click.stop>
          <ElDropdown v-if="activeTab === 'my-role'" trigger="click">
            <button type="button" class="role-picker__more">
              <IconifyIcon icon="lucide:ellipsis" class="size-4" />
            </button>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem @click="handlerCardEdit(role)">
                  编辑
                </ElDropdownItem>
                <ElDropdownItem @click="handlerCardDelete(role)">
                  <span class="text-[var(--el-color-danger)]">删除</span>
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
          <IconifyIcon
            v-else
            icon="lucide:chevron-right"
            class="role-picker__arrow size-4"
          />
        </div>
      </button>

      <p v-if="loading" class="role-picker__loading">加载中…</p>
    </div>
  </div>
</template>
