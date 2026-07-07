<script setup lang="ts">
import type { PropType } from 'vue';

import type { AiChatConversationApi } from '#/api/ai/chat/conversation';

import { computed, h, nextTick, onMounted, ref, toRefs, watch } from 'vue';

import { confirm, prompt } from '@vben/common-ui';
import { IconifyIcon, SvgGptIcon } from '@vben/icons';

import {
  ElAvatar,
  ElButton,
  ElEmpty,
  ElInput,
  ElMessage,
} from 'element-plus';

import {
  createChatConversationMy,
  deleteChatConversationMy,
  deleteChatConversationMyByUnpinned,
  getChatConversationMyList,
  updateChatConversationMy,
} from '#/api/ai/chat/conversation';
import { $t } from '#/locales';

import RolePickerPanel from '../role/role-picker-panel.vue';

import './conversation-sidebar.scss';

const props = defineProps({
  activeId: {
    type: [Number, null] as PropType<null | number>,
    default: null,
  },
});

const emits = defineEmits([
  'onConversationCreate',
  'onConversationClick',
  'onConversationClear',
  'onConversationDelete',
]);

type SidebarView = 'chats' | 'roles';

const sidebarView = ref<SidebarView>('chats');
const sidebarCollapsed = ref(false);
const searchName = ref<string>('');
const activeConversationId = ref<null | number>(null); // 选中的对话，默认为 null
const hoverConversationId = ref<null | number>(null); // 悬浮上去的对话
const conversationList = ref([] as AiChatConversationApi.ChatConversation[]); // 对话列表
const conversationMap = ref<any>({}); // 对话分组 (置顶、今天、三天前、一星期前、一个月前)
const loading = ref<boolean>(false); // 加载中
const loadingTime = ref<any>();

const railConversations = computed(() => {
  const items: AiChatConversationApi.ChatConversation[] = [];
  for (const key of Object.keys(conversationMap.value)) {
    const group = conversationMap.value[key] as AiChatConversationApi.ChatConversation[];
    for (const conversation of group) {
      items.push(conversation);
      if (items.length >= 8) {
        return items;
      }
    }
  }
  return items;
});

function scheduleLayoutResize() {
  void nextTick(() => {
    window.setTimeout(() => window.dispatchEvent(new Event('resize')), 280);
  });
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  scheduleLayoutResize();
}

function expandSidebar(view: SidebarView = 'chats') {
  sidebarCollapsed.value = false;
  sidebarView.value = view;
  scheduleLayoutResize();
}

/** 搜索对话 */
async function searchConversation() {
  // 恢复数据
  if (searchName.value.trim().length === 0) {
    conversationMap.value = await getConversationGroupByCreateTime(
      conversationList.value,
    );
  } else {
    // 过滤
    const filterValues = conversationList.value.filter((item) => {
      return item.title.includes(searchName.value.trim());
    });
    conversationMap.value =
      await getConversationGroupByCreateTime(filterValues);
  }
}

/** 点击对话 */
async function handleConversationClick(id: number) {
  // 过滤出选中的对话
  const filterConversation = conversationList.value.find((item) => {
    return item.id === id;
  });
  // 回调 onConversationClick
  // noinspection JSVoidFunctionReturnValueUsed
  const success = emits('onConversationClick', filterConversation) as any;
  // 切换对话
  if (success) {
    activeConversationId.value = id;
  }
}

/** 获取对话列表 */
async function getChatConversationList() {
  try {
    // 加载中
    loadingTime.value = setTimeout(() => {
      loading.value = true;
    }, 50);

    // 1.1 获取 对话数据
    conversationList.value = await getChatConversationMyList();
    // 1.2 排序
    conversationList.value.toSorted((a, b) => {
      return Number(b.createTime) - Number(a.createTime);
    });
    // 1.3 没有任何对话情况
    if (conversationList.value.length === 0) {
      activeConversationId.value = null;
      conversationMap.value = {};
      return;
    }

    // 2. 对话根据时间分组(置顶、今天、一天前、三天前、七天前、30 天前)
    conversationMap.value = await getConversationGroupByCreateTime(
      conversationList.value,
    );
  } finally {
    // 清理定时器
    if (loadingTime.value) {
      clearTimeout(loadingTime.value);
    }
    // 加载完成
    loading.value = false;
  }
}

/** 按照 creteTime 创建时间，进行分组 */
async function getConversationGroupByCreateTime(
  list: AiChatConversationApi.ChatConversation[],
) {
  // 排序、指定、时间分组(今天、一天前、三天前、七天前、30天前)
  // noinspection NonAsciiCharacters
  const groupMap: any = {
    置顶: [],
    今天: [],
    一天前: [],
    三天前: [],
    七天前: [],
    三十天前: [],
  };
  // 当前时间的时间戳
  const now = Date.now();
  // 定义时间间隔常量（单位：毫秒）
  const oneDay = 24 * 60 * 60 * 1000;
  const threeDays = 3 * oneDay;
  const sevenDays = 7 * oneDay;
  const thirtyDays = 30 * oneDay;
  for (const conversation of list) {
    // 置顶
    if (conversation.pinned) {
      groupMap['置顶'].push(conversation);
      continue;
    }
    // 计算时间差（单位：毫秒）
    const diff = now - Number(conversation.createTime);
    // 根据时间间隔判断
    if (diff < oneDay) {
      groupMap['今天'].push(conversation);
    } else if (diff < threeDays) {
      groupMap['一天前'].push(conversation);
    } else if (diff < sevenDays) {
      groupMap['三天前'].push(conversation);
    } else if (diff < thirtyDays) {
      groupMap['七天前'].push(conversation);
    } else {
      groupMap['三十天前'].push(conversation);
    }
  }
  return groupMap;
}

async function createConversation() {
  const conversationId = await createChatConversationMy(
    {} as unknown as AiChatConversationApi.ChatConversation,
  );
  await getChatConversationList();
  expandSidebar('chats');
  await handleConversationClick(conversationId);
  emits('onConversationCreate');
}

/** 修改对话的标题 */
async function updateConversationTitle(
  conversation: AiChatConversationApi.ChatConversation,
) {
  // 1. 二次确认
  await prompt({
    async beforeClose(scope) {
      if (scope.isConfirm) {
        if (scope.value) {
          try {
            // 2. 发起修改
            await updateChatConversationMy({
              id: conversation.id,
              title: scope.value,
            } as AiChatConversationApi.ChatConversation);
            ElMessage.success('重命名成功');
            // 3. 刷新列表
            await getChatConversationList();
            // 4. 过滤当前切换的
            const filterConversationList = conversationList.value.filter(
              (item) => {
                return item.id === conversation.id;
              },
            );
            if (
              filterConversationList.length > 0 &&
              filterConversationList[0] && // tip：避免切换对话
              activeConversationId.value === filterConversationList[0].id!
            ) {
              emits('onConversationClick', filterConversationList[0]);
            }
          } catch {
            return false;
          }
        } else {
          ElMessage.error('请输入标题');
          return false;
        }
      }
    },
    component: () => {
      return h(ElInput, {
        placeholder: '请输入标题',
        clearable: true,
        modelValue: conversation.title,
      });
    },
    content: '请输入标题',
    title: '修改标题',
    modelPropName: 'modelValue',
  });
}

/** 删除聊天对话 */
async function deleteChatConversation(
  conversation: AiChatConversationApi.ChatConversation,
) {
  // 删除的二次确认
  await confirm(`是否确认删除对话 - ${conversation.title}?`);
  // 发起删除
  await deleteChatConversationMy(conversation.id);
  ElMessage.success('对话已删除');
  // 刷新列表
  await getChatConversationList();
  // 回调
  emits('onConversationDelete', conversation);
}

/** 清空未置顶的对话 */
async function handleClearConversation() {
  await confirm('确认后对话会全部清空，置顶的对话除外。');
  await deleteChatConversationMyByUnpinned();
  ElMessage.success($t('ui.actionMessage.operationSuccess'));
  // 清空对话、对话内容
  activeConversationId.value = null;
  // 获取对话列表
  await getChatConversationList();
  // 回调 方法
  emits('onConversationClear');
}

/** 对话置顶 */
async function handleTop(conversation: AiChatConversationApi.ChatConversation) {
  // 更新对话置顶
  conversation.pinned = !conversation.pinned;
  await updateChatConversationMy(conversation);
  // 刷新对话
  await getChatConversationList();
}

// ============ 角色仓库（侧栏内嵌） ============

function openRolePicker() {
  expandSidebar('roles');
}

function closeRolePicker() {
  sidebarView.value = 'chats';
}

async function handleRoleUsed(conversationId: number) {
  await getChatConversationList();
  await handleConversationClick(conversationId);
  closeRolePicker();
}

/** 监听选中的对话 */
const { activeId } = toRefs(props);
watch(activeId, async (newValue) => {
  activeConversationId.value = newValue;
});

defineExpose({ createConversation });

/** 初始化 */
onMounted(async () => {
  // 获取 对话列表
  await getChatConversationList();
  // 默认选中
  if (props.activeId) {
    activeConversationId.value = props.activeId;
  } else {
    // 首次默认选中第一个
    if (conversationList.value.length > 0 && conversationList.value[0]) {
      activeConversationId.value = conversationList.value[0].id;
      // 回调 onConversationClick
      emits('onConversationClick', conversationList.value[0]);
    }
  }
});
</script>

<template>
  <aside
    class="conversation-aside"
    :class="{ 'is-collapsed': sidebarCollapsed }"
  >
    <button
      type="button"
      class="conversation-aside__toggle"
      :title="sidebarCollapsed ? '展开对话侧栏' : '收起对话侧栏'"
      @click="toggleSidebar"
    >
      <IconifyIcon
        :icon="sidebarCollapsed ? 'lucide:chevron-right' : 'lucide:chevron-left'"
        class="size-4"
      />
    </button>

    <div v-if="sidebarCollapsed" class="conversation-aside__rail">
      <button
        type="button"
        class="conversation-aside__rail-btn conversation-aside__rail-btn--primary"
        title="新建对话"
        @click="createConversation"
      >
        <IconifyIcon icon="lucide:plus" class="size-4" />
      </button>

      <button
        type="button"
        class="conversation-aside__rail-btn"
        title="角色仓库"
        @click="openRolePicker"
      >
        <IconifyIcon icon="lucide:library" class="size-4" />
      </button>

      <div class="conversation-aside__rail-divider" aria-hidden="true"></div>

      <button
        v-for="conversation in railConversations"
        :key="conversation.id"
        type="button"
        class="conversation-aside__rail-chat"
        :class="{ 'is-active': conversation.id === activeConversationId }"
        :title="conversation.title"
        @click="handleConversationClick(conversation.id)"
      >
        <ElAvatar
          v-if="conversation.roleAvatar"
          :src="conversation.roleAvatar"
          :size="28"
        />
        <span v-else class="conversation-aside__rail-chat-fallback">
          <SvgGptIcon class="size-4" />
        </span>
      </button>
    </div>

    <div v-else class="conversation-sidebar">
      <RolePickerPanel
        v-if="sidebarView === 'roles'"
        @back="closeRolePicker"
        @role-used="handleRoleUsed"
      />

      <template v-else>
        <div class="conversation-sidebar__actions">
          <ElButton
            class="conversation-sidebar__new-btn h-10"
            type="primary"
            @click="createConversation"
          >
            <IconifyIcon icon="lucide:plus" class="mr-1.5" />
            新建对话
          </ElButton>
          <button
            type="button"
            class="conversation-sidebar__role-btn"
            @click="openRolePicker"
          >
            <IconifyIcon icon="lucide:library" class="size-4" />
            角色仓库
          </button>
        </div>

        <ElInput
          v-model="searchName"
          size="default"
          class="conversation-sidebar__search"
          placeholder="搜索历史记录"
          @keyup="searchConversation"
        >
          <template #prefix>
            <IconifyIcon
              icon="lucide:search"
              class="text-[var(--el-text-color-placeholder)]"
            />
          </template>
        </ElInput>

        <div class="conversation-sidebar__list">
          <ElEmpty v-if="loading" description="." v-loading="loading" />

          <div
            v-for="conversationKey in Object.keys(conversationMap)"
            :key="conversationKey"
          >
            <div
              v-if="conversationMap[conversationKey].length > 0"
              class="conversation-sidebar__group-title"
            >
              {{ conversationKey }}
            </div>

            <div
              v-for="conversation in conversationMap[conversationKey]"
              :key="conversation.id"
              @click="handleConversationClick(conversation.id)"
              @mouseover="hoverConversationId = conversation.id"
              @mouseout="hoverConversationId = null"
            >
              <div
                class="conversation-sidebar__item"
                :class="{ 'is-active': conversation.id === activeConversationId }"
              >
                <div class="conversation-sidebar__item-main">
                  <ElAvatar
                    v-if="conversation.roleAvatar"
                    :src="conversation.roleAvatar"
                    :size="28"
                  />
                  <SvgGptIcon v-else class="size-6 shrink-0" />
                  <span class="conversation-sidebar__item-title max-w-[168px]">
                    {{ conversation.title }}
                  </span>
                </div>

                <div
                  v-show="hoverConversationId === conversation.id"
                  class="conversation-sidebar__item-actions"
                >
                  <ElButton
                    class="!px-1"
                    link
                    @click.stop="handleTop(conversation)"
                  >
                    <IconifyIcon
                      v-if="!conversation.pinned"
                      icon="lucide:arrow-up-to-line"
                    />
                    <IconifyIcon
                      v-if="conversation.pinned"
                      icon="lucide:arrow-down-from-line"
                    />
                  </ElButton>
                  <ElButton
                    class="!px-1"
                    link
                    @click.stop="updateConversationTitle(conversation)"
                  >
                    <IconifyIcon icon="lucide:edit" />
                  </ElButton>
                  <ElButton
                    class="!px-1"
                    link
                    @click.stop="deleteChatConversation(conversation)"
                  >
                    <IconifyIcon icon="lucide:trash-2" />
                  </ElButton>
                </div>
              </div>
            </div>
          </div>
        </div>

        <footer class="conversation-sidebar__footer">
          <button
            type="button"
            class="conversation-sidebar__clear-btn"
            title="清空未置顶对话"
            @click="handleClearConversation"
          >
            <IconifyIcon icon="lucide:brush-cleaning" class="size-3.5" />
            清空未置顶对话
          </button>
        </footer>
      </template>
    </div>
  </aside>
</template>
