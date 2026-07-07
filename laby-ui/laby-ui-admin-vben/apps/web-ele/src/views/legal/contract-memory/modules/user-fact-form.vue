<script lang="ts" setup>
import type { LegalContractMemoryApi } from '#/api/legal/contract-memory';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createUserFact, updateUserFact } from '#/api/legal/contract-memory';
import { useUserStore } from '@vben/stores';

const emit = defineEmits<{ success: [] }>();

const userStore = useUserStore();
const isUpdate = ref(false);
const rowData = ref<LegalContractMemoryApi.UserFact>();

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  layout: 'horizontal',
  schema: [
    {
      fieldName: 'userId',
      label: '用户编号',
      component: 'InputNumber',
      rules: 'required',
      componentProps: { controlsPosition: 'right' },
    },
    {
      fieldName: 'contractId',
      label: '合同编号',
      component: 'InputNumber',
      componentProps: { controlsPosition: 'right' },
    },
    {
      fieldName: 'sessionId',
      label: '会话编号',
      component: 'Input',
    },
    {
      fieldName: 'content',
      label: '内容',
      component: 'Input',
      rules: 'required',
      componentProps: { type: 'textarea', rows: 4 },
    },
  ],
  showDefaultActions: false,
});

const title = computed(() => (isUpdate.value ? '编辑用户事实' : '新增用户事实'));

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    try {
      const values = await formApi.getValues();
      if (isUpdate.value && rowData.value?.id) {
        await updateUserFact({
          id: rowData.value.id,
          userId: values.userId,
          contractId: values.contractId,
          sessionId: values.sessionId,
          content: values.content,
        });
        ElMessage.success('已更新');
      } else {
        await createUserFact({
          userId: values.userId,
          contractId: values.contractId,
          sessionId: values.sessionId,
          content: values.content,
        });
        ElMessage.success('已创建');
      }
      await modalApi.close();
      emit('success');
    } finally {
      modalApi.unlock();
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData<LegalContractMemoryApi.UserFact>();
    rowData.value = data ?? undefined;
    isUpdate.value = !!data?.id;
    formApi.resetForm();
    if (data) {
      formApi.setValues(data);
    } else {
      formApi.setValues({ userId: userStore.userInfo?.id });
    }
  },
});
</script>

<template>
  <Modal :title="title">
    <Form />
  </Modal>
</template>
