<script lang="ts" setup>
import type { AiModelChatRoleApi } from '#/api/ai/model/chatRole';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  ElButton,
  ElDialog,
  ElInput,
  ElMessage,
} from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  createChatRole,
  getChatRole,
  polishChatRoleSystemMessage,
  updateChatRole,
} from '#/api/ai/model/chatRole';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

/** 多选字段：单选时 ElSelect 可能返回标量，提交前统一为数组 */
function normalizeIdList(value: unknown): number[] | undefined {
  if (value == null || value === '') {
    return undefined;
  }
  if (Array.isArray(value)) {
    return value.map(Number).filter((n) => !Number.isNaN(n));
  }
  const n = Number(value);
  return Number.isNaN(n) ? undefined : [n];
}

function normalizeStringList(value: unknown): string[] | undefined {
  if (value == null || value === '') {
    return undefined;
  }
  if (Array.isArray(value)) {
    return value.map(String);
  }
  return [String(value)];
}

function normalizeChatRolePayload(
  data: AiModelChatRoleApi.ChatRole,
): AiModelChatRoleApi.ChatRole {
  return {
    ...data,
    knowledgeIds: normalizeIdList(data.knowledgeIds),
    toolIds: normalizeIdList(data.toolIds),
    mcpClientNames: normalizeStringList(data.mcpClientNames),
  };
}

const emit = defineEmits(['success']);
const formData = ref<AiModelChatRoleApi.ChatRole>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['聊天角色'])
    : $t('ui.actionTitle.create', ['聊天角色']);
});

const polishVisible = ref(false);
const polishDraft = ref('');
const polishLoading = ref(false);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-1 items-start',
    labelWidth: 96,
  },
  layout: 'horizontal',
  wrapperClass: 'grid grid-cols-2 gap-x-6 gap-y-0',
  schema: useFormSchema(),
  showDefaultActions: false,
});

async function openPolishDialog() {
  const values = await formApi.getValues();
  polishDraft.value = '';
  polishVisible.value = true;
  if (values?.systemMessage) {
    polishDraft.value = `在现有设定基础上优化：\n${values.systemMessage}`;
  }
}

async function handlePolish() {
  if (!polishDraft.value.trim()) {
    ElMessage.warning('请填写润色需求或草稿');
    return;
  }
  const values = await formApi.getValues();
  polishLoading.value = true;
  try {
    const res = await polishChatRoleSystemMessage({
      draft: polishDraft.value.trim(),
      scene: values?.category
        ? `${values.category}${values.name ? ` / ${values.name}` : ''}`
        : '聊天角色',
      modelId: values?.modelId,
      existingSystemMessage: values?.systemMessage,
    });
    const text =
      typeof res === 'string' ? res : (res as { systemMessage?: string })?.systemMessage;
    if (!text) {
      ElMessage.error('未返回润色结果');
      return;
    }
    await formApi.setFieldValue('systemMessage', text);
    polishVisible.value = false;
    ElMessage.success('已写入角色设定，请确认后保存');
  } finally {
    polishLoading.value = false;
  }
}

const [Modal, modalApi] = useVbenModal({
  contentClass: 'overflow-hidden',
  class: 'w-[960px] max-w-[96vw] !max-h-none',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const data = normalizeChatRolePayload(
      (await formApi.getValues()) as AiModelChatRoleApi.ChatRole,
    );
    try {
      await (formData.value?.id ? updateChatRole(data) : createChatRole(data));
      await modalApi.close();
      emit('success');
      ElMessage.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }
    const data = modalApi.getData<AiModelChatRoleApi.ChatRole>();
    if (!data || !data.id) {
      await formApi.setValues(data);
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getChatRole(data.id);
      await formApi.setValues({ ...data, ...formData.value });
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="chat-role-form-modal px-1">
      <Form />
    </div>

    <template #prepend-footer>
      <ElButton link type="primary" @click="openPolishDialog">
        AI 润色角色设定
      </ElButton>
    </template>

    <ElDialog
      v-model="polishVisible"
      title="AI 润色提示词"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <p class="mb-2 text-sm text-gray-500">
        描述审查重点、输出格式、语气等，AI 将生成可填入「角色设定」的 system 提示词。
      </p>
      <ElInput
        v-model="polishDraft"
        type="textarea"
        :rows="8"
        placeholder="例如：站在甲方立场审采购合同，只输出 JSON 意见数组，每批最多 5 条…"
      />
      <template #footer>
        <ElButton @click="polishVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="polishLoading" @click="handlePolish">
          生成并填入
        </ElButton>
      </template>
    </ElDialog>
  </Modal>
</template>

<style scoped>
/* 弹窗随内容撑开，内容区不出现纵向滚轮 */
.chat-role-form-modal :deep(.flex-auto.overflow-hidden) {
  overflow: visible !important;
}

.chat-role-form-modal :deep(.relative.flex.pb-4) {
  padding-bottom: 0.65rem !important;
}

.chat-role-form-modal :deep(p.text-destructive) {
  position: static !important;
  margin-top: 2px;
  line-height: 1.3;
  font-size: 12px;
}

.chat-role-form-modal :deep(.el-textarea__inner) {
  overflow-y: hidden;
}
</style>
