<script lang="ts" setup>
import type { LegalSkillPackApi } from '#/api/legal/skill-pack';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  createSkillPack,
  getSkillPack,
  updateSkillPack,
} from '#/api/legal/skill-pack';
import { $t } from '#/locales';

import {
  LEGAL_RULES_FORM_GRID,
  LEGAL_RULES_FORM_MODAL_OPTIONS,
} from '../../components/legal-rules-form-modal';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<LegalSkillPackApi.SkillPack>();
const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['AI 技能包'])
    : $t('ui.actionTitle.create', ['AI 技能包']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: LEGAL_RULES_FORM_GRID.formItemClass,
    labelWidth: 110,
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
    const data = (await formApi.getValues()) as LegalSkillPackApi.SkillPack;
    try {
      await (formData.value?.id ? updateSkillPack(data) : createSkillPack(data));
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
    const data = modalApi.getData<LegalSkillPackApi.SkillPack>();
    if (!data?.id) {
      await formApi.resetForm();
      await formApi.setValues({ enabled: true, scene: 'AUDIT' });
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getSkillPack(data.id);
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
