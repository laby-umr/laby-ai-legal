<script lang="ts" setup>
import type { LegalContractTypeApi } from '#/api/legal/contract-type';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  createContractType,
  getContractType,
  updateContractType,
} from '#/api/legal/contract-type';
import { $t } from '#/locales';

import {
  LEGAL_RULES_FORM_GRID,
  LEGAL_RULES_FORM_MODAL_OPTIONS,
} from '../../components/legal-rules-form-modal';
import { useFormSchema } from '../data';
import ConfigOverviewPanel from './config-overview-panel.vue';

const emit = defineEmits(['success']);
const formData = ref<LegalContractTypeApi.ContractType>();
const overviewRef = ref<InstanceType<typeof ConfigOverviewPanel>>();
const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['合同类型'])
    : $t('ui.actionTitle.create', ['合同类型']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: LEGAL_RULES_FORM_GRID.formItemClass,
    labelWidth: LEGAL_RULES_FORM_GRID.labelWidth,
  },
  layout: 'horizontal',
  wrapperClass: LEGAL_RULES_FORM_GRID.wrapperClass,
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  ...LEGAL_RULES_FORM_MODAL_OPTIONS,
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const data = (await formApi.getValues()) as LegalContractTypeApi.ContractType;
    try {
      await (formData.value?.id
        ? updateContractType(data)
        : createContractType(data));
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
    const data = modalApi.getData<LegalContractTypeApi.ContractType>();
    if (!data?.id) {
      await formApi.resetForm();
      await formApi.setValues({ status: 0, sort: 0 });
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getContractType(data.id);
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="legal-rules-form-modal px-4">
      <ConfigOverviewPanel
        v-if="formData?.id"
        ref="overviewRef"
        :contract-type-id="formData.id"
        class="mb-3"
      />
      <Form />
    </div>
  </Modal>
</template>

<style scoped>
.legal-rules-form-modal :deep(.flex-auto.overflow-hidden) {
  overflow: visible !important;
}

.legal-rules-form-modal :deep(p.text-destructive) {
  position: static !important;
  margin-top: 2px;
  line-height: 1.3;
  font-size: 12px;
}

.legal-rules-form-modal :deep(.relative.flex.pb-4) {
  padding-bottom: 0.75rem !important;
}
</style>
