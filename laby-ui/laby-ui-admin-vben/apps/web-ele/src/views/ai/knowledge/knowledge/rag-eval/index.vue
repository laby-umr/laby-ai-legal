<script setup lang="ts">
import type {
  AiKnowledgeRagEvalApi,
  AiKnowledgeRagEvalLiveCaseApi,
} from '#/api/ai/knowledge/knowledge';

import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElCard,
  ElEmpty,
  ElMessage,
  ElProgress,
  ElTag,
} from 'element-plus';

import {
  getKnowledge,
  getRagEvalLiveCases,
  runKnowledgeRagEval,
} from '#/api/ai/knowledge/knowledge';

defineOptions({ name: 'KnowledgeRagEval' });

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const knowledgeId = ref<number>();
const knowledgeName = ref('');
const liveCases = ref<AiKnowledgeRagEvalLiveCaseApi.LiveCase[]>([]);
const report = ref<AiKnowledgeRagEvalApi.Report | null>(null);

const passPercent = computed(() =>
  report.value ? Math.round(report.value.passRate * 100) : 0,
);

const hitAtKPercent = computed(() =>
  report.value ? Math.round(report.value.hitAtKRate * 100) : 0,
);

async function loadKnowledgeInfo(id: number) {
  const knowledge = await getKnowledge(id);
  knowledgeName.value = knowledge?.name ?? `知识库 #${id}`;
}

async function handleRunEval() {
  if (!knowledgeId.value) {
    return;
  }
  loading.value = true;
  report.value = null;
  try {
    report.value = await runKnowledgeRagEval(knowledgeId.value);
    if (report.value.failedCaseIds?.length) {
      ElMessage.warning(
        `测评完成：${report.value.passedCases}/${report.value.totalCases} 通过`,
      );
    } else {
      ElMessage.success('测评全部通过');
    }
  } finally {
    loading.value = false;
  }
}

function goVectorHealth() {
  router.push({
    name: 'AiKnowledgeVectorHealth',
    query: { id: knowledgeId.value },
  });
}

onMounted(async () => {
  const id = Number(route.query.id);
  if (!id) {
    ElMessage.error('知识库 ID 不存在，无法进行 RAG 测评');
    router.back();
    return;
  }
  knowledgeId.value = id;
  try {
    await Promise.all([loadKnowledgeInfo(id), loadLiveCases()]);
  } catch {}
});

async function loadLiveCases() {
  liveCases.value = await getRagEvalLiveCases();
}
</script>

<template>
  <Page auto-content-height>
    <div class="flex w-full flex-col gap-4 lg:flex-row">
      <ElCard class="lg:w-80">
        <h3 class="mb-2 text-lg font-semibold">RAG 黄金集测评</h3>
        <p class="mb-4 text-sm text-gray-500">
          对知识库
          <span class="font-medium text-gray-700">{{ knowledgeName }}</span>
          运行在线黄金集（`rag-cases-live.json`），验证真实检索链路是否退化。
        </p>
        <ul class="mb-4 list-disc space-y-1 pl-5 text-xs text-gray-500">
          <li>需已完成文档入库与向量化</li>
          <li>用例以 TopK 内容关键词命中为主</li>
          <li>测评前建议先跑「向量检查」</li>
        </ul>
        <div
          v-if="liveCases.length"
          class="mb-4 max-h-48 space-y-2 overflow-y-auto rounded bg-gray-50 p-2 text-xs"
        >
          <div class="font-medium text-gray-600">
            默认用例（{{ liveCases.length }}）
          </div>
          <div
            v-for="item in liveCases"
            :key="item.caseId"
            class="text-gray-500"
          >
            <span class="text-gray-700">{{ item.caseId }}</span>
            · {{ item.query }}
          </div>
        </div>
        <ElButton
          type="primary"
          class="mb-2 w-full"
          :loading="loading"
          @click="handleRunEval"
        >
          开始测评
        </ElButton>
        <ElButton class="w-full" text @click="goVectorHealth">
          前往向量检查
        </ElButton>
      </ElCard>

      <ElCard class="min-w-0 flex-1">
        <template v-if="loading">
          <div class="flex h-72 items-center justify-center">
            <ElEmpty description="测评进行中..." />
          </div>
        </template>

        <template v-else-if="report">
          <div class="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">通过率</div>
              <div class="text-xl font-semibold">
                {{ report.passedCases }}/{{ report.totalCases }}
              </div>
              <ElProgress
                :percentage="passPercent"
                :status="passPercent === 100 ? 'success' : 'warning'"
                :stroke-width="8"
                class="mt-2"
              />
            </div>
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">Hit@K</div>
              <div class="text-xl font-semibold">
                {{ report.hitAtKCases }}/{{ report.totalCases }}
              </div>
              <ElProgress
                :percentage="hitAtKPercent"
                :stroke-width="8"
                class="mt-2"
              />
            </div>
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">平均 MRR</div>
              <div class="text-xl font-semibold">
                {{ report.avgMrr?.toFixed(3) ?? '-' }}
              </div>
            </div>
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">平均 Recall@K</div>
              <div class="text-xl font-semibold">
                {{ report.avgRecallAtK?.toFixed(3) ?? '-' }}
              </div>
            </div>
          </div>

          <div
            v-if="report.failedCaseIds?.length"
            class="mb-4 rounded border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800"
          >
            失败用例：
            <ElTag
              v-for="caseId in report.failedCaseIds"
              :key="caseId"
              size="small"
              type="warning"
              class="mr-1"
            >
              {{ caseId }}
            </ElTag>
          </div>

          <div class="space-y-2">
            <div
              v-for="item in report.caseResults"
              :key="item.caseId"
              class="rounded border border-solid border-gray-200 p-3"
            >
              <div class="mb-1 flex flex-wrap items-center gap-2">
                <ElTag :type="item.pass ? 'success' : 'danger'" size="small">
                  {{ item.pass ? '通过' : '失败' }}
                </ElTag>
                <span class="font-medium">{{ item.caseId }}</span>
                <span class="text-sm text-gray-500">{{ item.description }}</span>
              </div>
              <div class="text-xs text-gray-500">
                MRR {{ item.mrr?.toFixed(3) }} · Recall@K
                {{ item.recallAtK?.toFixed(3) }}
                <span v-if="item.topScore != null">
                  · Top1 {{ item.topScore }}
                </span>
                <span v-if="item.retrievedSegmentIds?.length">
                  · 召回 {{ item.retrievedSegmentIds.length }} 段
                </span>
              </div>
              <div
                v-if="item.failureReason"
                class="mt-1 text-xs text-red-600"
              >
                {{ item.failureReason }}
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="flex h-72 flex-col items-center justify-center gap-2">
            <IconifyIcon icon="lucide:flask-conical" :size="32" class="text-gray-300" />
            <ElEmpty description="点击左侧「开始测评」运行黄金集" />
          </div>
        </template>
      </ElCard>
    </div>
  </Page>
</template>
