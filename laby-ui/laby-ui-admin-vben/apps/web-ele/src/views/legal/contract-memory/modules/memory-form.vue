<script lang="ts" setup>
import type { LegalContractMemoryApi } from '#/api/legal/contract-memory';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  createContractMemoryPage,
  updateContractMemoryPage,
} from '#/api/legal/contract-memory';

import { MEMORY_TYPE_OPTIONS } from '../data';

const emit = defineEmits<{ success: [] }>();

const isUpdate = ref(false);
const rowData = ref<LegalContractMemoryApi.Memory>();

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  layout: 'horizontal',
  schema: [
    {
      fieldName: 'contractId',
      label: '合同编号',
      component: 'InputNumber',
      rules: 'required',
      componentProps: { controlsPosition: 'right' },
    },
    {
      fieldName: 'sessionId',
      label: '会话编号',
      component: 'Input',
    },
    {
      fieldName: 'memoryType',
      label: '记忆类型',
      component: 'Select',
      rules: 'required',
      componentProps: {
        options: MEMORY_TYPE_OPTIONS.filter((o) => o.value !== 'fact'),
      },
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

const title = computed(() => (isUpdate.value ? '编辑情节记忆' : '新增情节记忆'));

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
        await updateContractMemoryPage({
          id: rowData.value.id,
          contractId: values.contractId,
          memoryType: values.memoryType,
          content: values.content,
        });
        ElMessage.success('已更新');
      } else {
        await createContractMemoryPage({
          contractId: values.contractId,
          sessionId: values.sessionId,
          memoryType: values.memoryType,
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
    const data = modalApi.getData<LegalContractMemoryApi.Memory>();
    rowData.value = data ?? undefined;
    isUpdate.value = !!data?.id;
    formApi.resetForm();
    if (data) {
      formApi.setValues(data);
    }
  },
});
</script>

<template>
  <Modal :title="title">
    <Form />
  </Modal>
</template>
