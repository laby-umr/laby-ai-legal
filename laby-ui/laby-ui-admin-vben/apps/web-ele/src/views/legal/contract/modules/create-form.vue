<script lang="ts" setup>
import type { UploadFile, UploadRawFile } from 'element-plus';

import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import { useAccess } from '@vben/access';
import { useVbenModal } from '@vben/common-ui';
import { AiModelTypeEnum, AiPlatformEnum } from '@vben/constants';
import { IconifyIcon } from '@vben/icons';

import { ElButton, ElMessage, ElUpload } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  getChatRolePage,
  LEGAL_CONTRACT_CHAT_ROLE_CATEGORY,
} from '#/api/ai/model/chatRole';
import { getModelSimpleList } from '#/api/ai/model/model';
import {
  createContract,
  uploadContractFile,
} from '#/api/legal/contract';
import { getContractTypeSimpleList } from '#/api/legal/contract-type';
import { useUpload } from '#/components/upload/use-upload';

import { MAX_CONTRACT_FILE_SIZE_MB } from '../constants';
import { useCreateFormSchema } from '../create/data';

const emit = defineEmits(['success']);

const router = useRouter();
const { uploadUrl } = useUpload('legal-contract');
const { hasAccessByCodes } = useAccess();
const showAdvancedPrompt = computed(() =>
  hasAccessByCodes(['legal:contract:advanced', 'legal:contract:update']),
);

const pendingFile = ref<File>();
const fileList = ref<UploadFile[]>([]);
const round1RoleOptions = ref<{ label: string; value: number; modelId?: number }[]>(
  [],
);
const round2RoleOptions = ref<{ label: string; value: number }[]>([]);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-1 items-start',
    labelWidth: 88,
  },
  layout: 'horizontal',
  wrapperClass: 'grid grid-cols-2 gap-x-4',
  schema: useCreateFormSchema(),
  showDefaultActions: false,
});

function resetFormState() {
  pendingFile.value = undefined;
  fileList.value = [];
  formApi.resetForm();
}

function isReauditRoleName(name?: string) {
  return name?.includes('二轮') ?? false;
}

function openChatRoleAdmin() {
  modalApi.close();
  router.push({
    name: 'LegalContractPromptSettings',
    query: { category: LEGAL_CONTRACT_CHAT_ROLE_CATEGORY },
  });
}

async function loadOptions() {
  try {
    const contractTypes = await getContractTypeSimpleList();
    formApi.updateSchema([
      {
        fieldName: 'contractTypeId',
        componentProps: {
          options: (contractTypes || []).map((t) => ({
            label: t.name,
            value: t.id,
          })),
        },
      },
    ]);

    const list = await getModelSimpleList(AiModelTypeEnum.CHAT);
    const modelOptions = (list || [])
      .map((m) => ({
        label: `${m.name}（${m.model}）`,
        value: m.id,
        platform: m.platform,
        sort: m.sort ?? 0,
      }))
      .sort((a, b) => a.sort - b.sort);
    await formApi.updateSchema([
      {
        fieldName: 'modelId',
        componentProps: { options: modelOptions },
      },
    ]);
    const deepSeek = modelOptions.find(
      (m) => m.platform === AiPlatformEnum.DEEP_SEEK,
    );
    await formApi.setFieldValue(
      'modelId',
      deepSeek?.value ?? modelOptions[0]?.value,
    );
  } catch {
    ElMessage.warning('加载 AI 模型列表失败，将使用系统默认模型');
  }

  try {
    const { list } = await getChatRolePage({
      pageNo: 1,
      pageSize: 100,
      category: LEGAL_CONTRACT_CHAT_ROLE_CATEGORY,
      publicStatus: true,
    });
    round1RoleOptions.value = (list || [])
      .filter((r) => !isReauditRoleName(r.name))
      .map((r) => ({ label: r.name, value: r.id, modelId: r.modelId }));
    round2RoleOptions.value = (list || [])
      .filter((r) => isReauditRoleName(r.name))
      .map((r) => ({ label: r.name, value: r.id }));

    await formApi.updateSchema([
      {
        fieldName: 'auditRoleId',
        componentProps: {
          options: round1RoleOptions.value,
          onChange: onAuditRoleChange,
        },
        dependencies: {
          triggerFields: [''],
          show: () => showAdvancedPrompt.value,
        },
      },
      {
        fieldName: 'reauditRoleId',
        componentProps: { options: round2RoleOptions.value },
        dependencies: {
          triggerFields: ['reauditRoleId'],
          show: () =>
            showAdvancedPrompt.value && round2RoleOptions.value.length > 0,
        },
      },
    ]);
  } catch {
    ElMessage.warning('加载审核提示词角色失败，将使用系统内置提示词');
  }
}

const allowedExtensions = new Set(['doc', 'docx']);

function validateContractFile(file: UploadRawFile) {
  const name = file.name.toLowerCase();
  const ext = name.slice(Math.max(0, name.lastIndexOf('.') + 1));
  if (!allowedExtensions.has(ext)) {
    ElMessage.error('仅支持 .doc、.docx 格式');
    return false;
  }
  if (file.size / 1024 / 1024 > MAX_CONTRACT_FILE_SIZE_MB) {
    ElMessage.error(`文件大小不能超过 ${MAX_CONTRACT_FILE_SIZE_MB} MB`);
    return false;
  }
  return true;
}

function handleFileChange(uploadFile: UploadFile) {
  if (!uploadFile.raw) {
    return;
  }
  if (!validateContractFile(uploadFile.raw)) {
    fileList.value = [];
    pendingFile.value = undefined;
    formApi.setFieldValue('contractFile', undefined);
    return;
  }
  pendingFile.value = uploadFile.raw;
  fileList.value = [uploadFile];
  formApi.setFieldValue('contractFile', uploadFile.raw);
}

function handleFileRemove() {
  pendingFile.value = undefined;
  fileList.value = [];
  formApi.setFieldValue('contractFile', undefined);
}

function beforeUpload(file: UploadRawFile) {
  if (!validateContractFile(file)) {
    return false;
  }
  return false;
}

function handleExceed() {
  ElMessage.warning('仅可上传一份主合同文件');
}

async function onAuditRoleChange(roleId: number) {
  const role = round1RoleOptions.value.find((r) => r.value === roleId);
  if (role?.modelId) {
    await formApi.setFieldValue('modelId', role.modelId);
  }
}

const [Modal, modalApi] = useVbenModal({
  contentClass: 'overflow-hidden',
  class: 'w-[920px] max-w-[96vw] !max-h-none',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    if (!pendingFile.value) {
      ElMessage.warning('请上传合同文件（.doc / .docx）');
      return;
    }

    modalApi.lock();
    try {
      const values = await formApi.getValues();
      const uploadRes = await uploadContractFile(pendingFile.value);
      const fileId =
        typeof uploadRes === 'object' && uploadRes !== null
          ? Number((uploadRes as { fileId?: number }).fileId)
          : Number(uploadRes);
      const fileName =
        typeof uploadRes === 'object' &&
        uploadRes !== null &&
        'fileName' in uploadRes
          ? String((uploadRes as { fileName: string }).fileName)
          : pendingFile.value.name;

      if (!fileId || Number.isNaN(fileId)) {
        ElMessage.error('文件上传失败，未返回有效文件编号');
        return;
      }

      const id = await createContract({
        title: String(values.title).trim(),
        partyRole: values.partyRole,
        auditLevel: values.auditLevel,
        editable: values.editable ?? true,
        modelId: values.modelId,
        contractTypeId: values.contractTypeId ?? undefined,
        auditRoleId: showAdvancedPrompt.value ? values.auditRoleId : undefined,
        reauditRoleId: showAdvancedPrompt.value
          ? values.reauditRoleId
          : undefined,
        files: [{ fileId, fileName, mainFlag: true }],
      });
      await modalApi.close();
      emit('success');
      ElMessage.success(
        `已提交合同 #${id}，正在后台解析与 AI 审核，请在列表查看进度`,
        { duration: 5000 },
      );
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetFormState();
      return;
    }
    resetFormState();
    await loadOptions();
  },
});
</script>

<template>
  <Modal title="新建合同审核">
    <div class="legal-contract-create-modal mx-4">
      <Form>
        <template #contractFile>
          <div class="w-full">
            <ElUpload
              v-model:file-list="fileList"
              class="w-full"
              drag
              :action="uploadUrl"
              accept=".doc,.docx"
              :auto-upload="false"
              :limit="1"
              :before-upload="beforeUpload"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :on-exceed="handleExceed"
            >
              <div class="py-4 text-center">
                <IconifyIcon
                  icon="lucide:upload-cloud"
                  class="mx-auto mb-1 text-3xl text-muted-foreground"
                />
                <p class="text-sm">点击或拖拽上传合同文件</p>
                <p class="mt-1 text-xs text-muted-foreground">
                  .doc / .docx，≤ {{ MAX_CONTRACT_FILE_SIZE_MB }}MB
                </p>
              </div>
            </ElUpload>
          </div>
        </template>
      </Form>
    </div>
    <template #prepend-footer>
      <div v-if="showAdvancedPrompt" class="flex flex-auto items-center">
        <ElButton link type="primary" @click="openChatRoleAdmin">
          高级：管理提示词角色
        </ElButton>
      </div>
    </template>
  </Modal>
</template>

<style scoped>
/* 双列时校验文案占位，避免滚动条；弹窗高度随内容撑开 */
.legal-contract-create-modal :deep(.flex-auto.overflow-hidden) {
  overflow: visible !important;
}

.legal-contract-create-modal :deep(p.text-destructive) {
  position: static !important;
  margin-top: 2px;
  line-height: 1.3;
  font-size: 12px;
}

.legal-contract-create-modal :deep(.relative.flex.pb-4) {
  padding-bottom: 0.75rem !important;
}

.legal-contract-create-modal :deep(.el-upload-dragger) {
  padding: 12px 16px;
}
</style>
