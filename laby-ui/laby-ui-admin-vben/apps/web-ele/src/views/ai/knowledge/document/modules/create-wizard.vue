<script setup lang="ts">
import {
  getCurrentInstance,
  provide,
  ref,
} from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElStep, ElSteps } from 'element-plus';

import SplitStep from '../form/modules/split-step.vue';
import UploadStep from '../form/modules/upload-step.vue';

const emit = defineEmits(['success']);

const currentStep = ref(0);
const formData = ref({
  knowledgeId: undefined as number | undefined,
  id: undefined as number | undefined,
  segmentMaxTokens: 500,
  list: [] as Array<{
    count?: number;
    id: number;
    name: string;
    progress?: number;
    segments: Array<{
      content?: string;
      contentLength?: number;
      tokens?: number;
    }>;
    url: string;
  }>,
});

provide('parent', getCurrentInstance());

function resetWizard() {
  currentStep.value = 0;
  formData.value = {
    knowledgeId: undefined,
    id: undefined,
    segmentMaxTokens: 500,
    list: [],
  };
}

function goToNextStep() {
  if (currentStep.value < 1) {
    currentStep.value++;
  }
}

function goToPrevStep() {
  if (currentStep.value > 0) {
    currentStep.value--;
  }
}

function goToDocumentListAfterProcess() {
  modalApi.close();
  emit('success');
}

defineExpose({
  goToNextStep,
  goToPrevStep,
  goToDocumentListAfterProcess,
});

const [Modal, modalApi] = useVbenModal({
  contentClass: 'overflow-hidden',
  class: 'w-[980px] max-w-[96vw] !max-h-none',
  showConfirmButton: false,
  showCancelButton: false,
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetWizard();
      return;
    }
    const data = modalApi.getData<{ knowledgeId?: number }>();
    if (data?.knowledgeId) {
      formData.value.knowledgeId = data.knowledgeId;
    }
  },
});
</script>

<template>
  <Modal title="新建知识库文档">
    <div class="knowledge-doc-create-modal px-1">
      <ElSteps :active="currentStep" align-center class="mb-6">
        <ElStep title="上传文档" />
        <ElStep title="分段预览" />
      </ElSteps>

      <UploadStep v-if="currentStep === 0" v-model="formData" />
      <SplitStep v-else v-model="formData" />
    </div>
  </Modal>
</template>

<style scoped>
.knowledge-doc-create-modal :deep(.flex-auto.overflow-hidden) {
  overflow: visible !important;
}
</style>
