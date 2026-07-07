<script setup lang="ts">
import {
  getCurrentInstance,
  onBeforeUnmount,
  onMounted,
  provide,
  ref,
} from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { useTabs } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import { ElCard } from 'element-plus';

import { getKnowledgeDocument } from '#/api/ai/knowledge/document';
import { getKnowledgeSegmentProcessList } from '#/api/ai/knowledge/segment';
import {
  calcEmbeddingProgress,
  isEmbeddingComplete,
} from '#/views/ai/knowledge/document/utils/process';

import ProcessStep from './modules/process-step.vue';
import SplitStep from './modules/split-step.vue';
import UploadStep from './modules/upload-step.vue';

const route = useRoute();
const router = useRouter();

const uploadDocumentRef = ref();
const documentSegmentRef = ref();
const processCompleteRef = ref();
const currentStep = ref(0);
const steps = [
  { title: '上传文档' },
  { title: '文档分段' },
  { title: '处理并完成' },
];
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

const tabs = useTabs();

function handleBack() {
  tabs.closeCurrentTab();
  router.push({
    name: 'AiKnowledgeDocument',
    query: {
      knowledgeId: route.query.knowledgeId,
    },
  });
}

/** 保存并处理后：回到列表（列表页会继续轮询进度） */
function goToDocumentListAfterProcess() {
  tabs.closeCurrentTab();
  router.push({
    name: 'AiKnowledgeDocument',
    query: {
      knowledgeId: route.query.knowledgeId,
      fromProcess: '1',
    },
  });
}

function goToStep(stepIndex: number) {
  currentStep.value = Math.min(Math.max(stepIndex, 0), steps.length - 1);
}

function goToNextStep() {
  if (currentStep.value < steps.length - 1) {
    currentStep.value++;
  }
}

function goToPrevStep() {
  if (currentStep.value > 0) {
    currentStep.value--;
  }
}

async function shouldShowProcessStep(documentId: number) {
  try {
    const result = await getKnowledgeSegmentProcessList([documentId]);
    const info = result?.[0];
    if (!info?.count) {
      return false;
    }
    const progress = calcEmbeddingProgress(info.embeddingCount, info.count);
    return !isEmbeddingComplete(progress, info.count);
  } catch {
    return false;
  }
}

async function initData() {
  if (route.query.knowledgeId) {
    formData.value.knowledgeId = Number(route.query.knowledgeId);
  }

  const stepQuery = route.query.step;
  const documentId = route.query.id
    ? Number(route.query.id)
    : undefined;

  if (stepQuery === 'process' && documentId) {
    const document = await getKnowledgeDocument(documentId);
    formData.value.id = document.id;
    formData.value.segmentMaxTokens = document.segmentMaxTokens;
    const result = await getKnowledgeSegmentProcessList([documentId]);
    const info = result?.[0];
    const progress = calcEmbeddingProgress(info?.embeddingCount, info?.count);
    formData.value.list = [
      {
        id: document.id,
        name: document.name,
        url: document.url,
        segments: [],
        count: info?.count || 0,
        progress,
      },
    ];
    goToStep(2);
    return;
  }

  if (documentId) {
    const document = await getKnowledgeDocument(documentId);
    formData.value.id = document.id;
    formData.value.segmentMaxTokens = document.segmentMaxTokens;
    const result = await getKnowledgeSegmentProcessList([documentId]);
    const info = result?.[0];
    const progress = calcEmbeddingProgress(info?.embeddingCount, info?.count);
    formData.value.list = [
      {
        id: document.id,
        name: document.name,
        url: document.url,
        segments: [],
        count: info?.count || 0,
        progress,
      },
    ];
    if (info?.count && !isEmbeddingComplete(progress, info.count)) {
      goToStep(2);
    } else {
      goToStep(1);
    }
  }
}

onBeforeUnmount(() => {
  uploadDocumentRef.value = null;
  documentSegmentRef.value = null;
  processCompleteRef.value = null;
});

defineExpose({
  goToNextStep,
  goToPrevStep,
  handleBack,
  goToDocumentListAfterProcess,
});

onMounted(async () => {
  await initData();
});
</script>

<template>
  <Page auto-content-height>
    <div class="mx-auto">
      <div
        class="absolute left-0 right-0 top-0 z-10 flex h-12 items-center border-b bg-card px-4"
      >
        <div class="flex w-48 items-center overflow-hidden">
          <IconifyIcon
            icon="lucide:arrow-left"
            class="size-5 flex-shrink-0 cursor-pointer"
            @click="handleBack"
          />
          <span class="ml-2.5 truncate text-base">
            {{ formData.id ? '编辑知识库文档' : '创建知识库文档' }}
          </span>
        </div>

        <div class="flex h-full flex-1 items-center justify-center">
          <div class="flex h-full w-96 items-center justify-between">
            <div
              v-for="(step, index) in steps"
              :key="index"
              class="relative mx-4 flex h-full cursor-pointer items-center"
              :class="[
                currentStep === index
                  ? 'border-b-2 border-solid border-blue-500 text-blue-500'
                  : 'text-gray-500',
              ]"
              @click="goToStep(index)"
            >
              <div
                class="mr-2 flex h-7 w-7 items-center justify-center rounded-full border-2 border-solid text-base"
                :class="[
                  currentStep === index
                    ? 'border-blue-500 bg-blue-500 text-white'
                    : 'border-gray-300 bg-white text-gray-500',
                ]"
              >
                {{ index + 1 }}
              </div>
              <span class="whitespace-nowrap text-base font-bold">
                {{ step.title }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <ElCard :body-style="{ padding: '10px' }" class="mb-4">
        <div class="mt-12">
          <div v-if="currentStep === 0" class="mx-auto w-[560px]">
            <UploadStep v-model="formData" ref="uploadDocumentRef" />
          </div>
          <div v-if="currentStep === 1" class="mx-auto w-[980px] max-w-full">
            <SplitStep v-model="formData" ref="documentSegmentRef" />
          </div>
          <div v-if="currentStep === 2" class="mx-auto w-[560px]">
            <ProcessStep v-model="formData" ref="processCompleteRef" />
          </div>
        </div>
      </ElCard>
    </div>
  </Page>
</template>
