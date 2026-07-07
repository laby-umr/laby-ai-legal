<script setup lang="ts">
import type { AiKnowledgeVectorHealthApi } from '#/api/ai/knowledge/knowledge';

import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElCard,
  ElEmpty,
  ElMessage,
  ElMessageBox,
  ElSwitch,
  ElTag,
} from 'element-plus';

import {
  getKnowledge,
  runKnowledgeVectorHealthCheck,
} from '#/api/ai/knowledge/knowledge';

defineOptions({ name: 'KnowledgeVectorHealth' });

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const dryRun = ref(true);
const knowledgeId = ref<number>();
const documentId = ref<number>();
const scopeLabel = ref('');
const report = ref<AiKnowledgeVectorHealthApi.Report | null>(null);

const hasIssues = computed(() => {
  const r = report.value;
  if (!r) {
    return false;
  }
  return (
    (r.missingVectorId ?? 0) > 0 ||
    (r.missingInQdrant ?? 0) > 0 ||
    (r.modelMismatch ?? 0) > 0 ||
    (r.missingSparseText ?? 0) > 0 ||
    (r.repairFailed ?? 0) > 0
  );
});

const isHealthy = computed(
  () => report.value != null && !hasIssues.value && !report.value.warnings?.length,
);

async function loadKnowledgeInfo(id: number) {
  const knowledge = await getKnowledge(id);
  scopeLabel.value = knowledge?.name ?? `知识库 #${id}`;
}

async function handleRunCheck() {
  if (!knowledgeId.value && !documentId.value) {
    return;
  }
  if (!dryRun.value) {
    try {
      await ElMessageBox.confirm(
        '将尝试自动 re-embed 修复缺失向量，可能耗时较长。确认继续？',
        '自动修复',
        { type: 'warning' },
      );
    } catch {
      return;
    }
  }
  loading.value = true;
  report.value = null;
  try {
    report.value = await runKnowledgeVectorHealthCheck(
      knowledgeId.value,
      dryRun.value,
      documentId.value,
    );
    if (isHealthy.value) {
      ElMessage.success(dryRun.value ? '检查完成，未发现异常' : '修复完成，未发现残留异常');
    } else if (dryRun.value) {
      ElMessage.warning('检查完成，发现向量不一致项');
    } else {
      ElMessage.warning('修复完成，请查看明细');
    }
  } finally {
    loading.value = false;
  }
}

function goRagEval() {
  if (!knowledgeId.value) {
    return;
  }
  router.push({ name: 'AiKnowledgeRagEval', query: { id: knowledgeId.value } });
}

onMounted(async () => {
  const docId = Number(route.query.documentId);
  const kbId = Number(route.query.id);
  if (docId) {
    documentId.value = docId;
    scopeLabel.value = `文档 #${docId}`;
  } else if (kbId) {
    knowledgeId.value = kbId;
    try {
      await loadKnowledgeInfo(kbId);
    } catch {}
  } else {
    ElMessage.error('请指定知识库或文档编号');
    router.back();
  }
});
</script>

<template>
  <Page auto-content-height>
    <div class="flex w-full flex-col gap-4 lg:flex-row">
      <ElCard class="lg:w-80">
        <h3 class="mb-2 text-lg font-semibold">向量健康检查</h3>
        <p class="mb-4 text-sm text-gray-500">
          对
          <span class="font-medium text-gray-700">{{ scopeLabel }}</span>
          的 DB 分段与 Qdrant 向量进行对账
          <span v-if="documentId">（仅当前文档）</span>。
        </p>
        <div class="mb-4 flex items-center justify-between text-sm">
          <span class="text-gray-600">仅检查（dry-run）</span>
          <ElSwitch v-model="dryRun" />
        </div>
        <p class="mb-4 text-xs text-gray-400">
          关闭 dry-run 时将按配置上限自动 re-embed 修复缺失向量，并回填缺失的 sparse_text（Hybrid 检索）。
        </p>
        <ElButton
          type="primary"
          class="mb-2 w-full"
          :loading="loading"
          @click="handleRunCheck"
        >
          {{ dryRun ? '开始检查' : '检查并修复' }}
        </ElButton>
        <ElButton class="w-full" text :disabled="!knowledgeId" @click="goRagEval">
          前往 RAG 测评
        </ElButton>
      </ElCard>

      <ElCard class="min-w-0 flex-1">
        <template v-if="loading">
          <div class="flex h-72 items-center justify-center">
            <ElEmpty description="检查进行中..." />
          </div>
        </template>

        <template v-else-if="report">
          <div class="mb-4 flex flex-wrap items-center gap-2">
            <ElTag :type="isHealthy ? 'success' : 'warning'" size="small">
              {{ isHealthy ? '健康' : '存在异常' }}
            </ElTag>
            <ElTag v-if="report.dryRun" size="small" type="info">dry-run</ElTag>
            <span class="text-sm text-gray-500">
              扫描 {{ report.segmentScanned ?? 0 }} 个启用分段
            </span>
          </div>

          <div class="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">缺少 vector_id</div>
              <div
                class="text-xl font-semibold"
                :class="{ 'text-amber-600': (report.missingVectorId ?? 0) > 0 }"
              >
                {{ report.missingVectorId ?? 0 }}
              </div>
            </div>
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">Qdrant 缺失</div>
              <div
                class="text-xl font-semibold"
                :class="{ 'text-amber-600': (report.missingInQdrant ?? 0) > 0 }"
              >
                {{ report.missingInQdrant ?? 0 }}
              </div>
            </div>
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">模型不一致</div>
              <div
                class="text-xl font-semibold"
                :class="{ 'text-amber-600': (report.modelMismatch ?? 0) > 0 }"
              >
                {{ report.modelMismatch ?? 0 }}
              </div>
            </div>
            <div class="rounded-lg bg-gray-50 p-3">
              <div class="text-xs text-gray-500">缺少 sparse_text</div>
              <div
                class="text-xl font-semibold"
                :class="{ 'text-amber-600': (report.missingSparseText ?? 0) > 0 }"
              >
                {{ report.missingSparseText ?? 0 }}
              </div>
            </div>
            <div
              v-if="!report.dryRun"
              class="rounded-lg bg-gray-50 p-3"
            >
              <div class="text-xs text-gray-500">sparse 已回填</div>
              <div class="text-xl font-semibold text-green-600">
                {{ report.sparseTextRepaired ?? 0 }}
              </div>
            </div>
            <div
              v-if="!report.dryRun"
              class="rounded-lg bg-gray-50 p-3"
            >
              <div class="text-xs text-gray-500">向量已修复</div>
              <div class="text-xl font-semibold text-green-600">
                {{ report.repaired ?? 0 }}
              </div>
            </div>
            <div
              v-if="!report.dryRun"
              class="rounded-lg bg-gray-50 p-3"
            >
              <div class="text-xs text-gray-500">修复失败</div>
              <div
                class="text-xl font-semibold"
                :class="{ 'text-red-600': (report.repairFailed ?? 0) > 0 }"
              >
                {{ report.repairFailed ?? 0 }}
              </div>
            </div>
          </div>

          <div
            v-if="report.warnings?.length"
            class="space-y-1 rounded border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800"
          >
            <div v-for="(warning, index) in report.warnings" :key="index">
              {{ warning }}
            </div>
          </div>
        </template>

        <template v-else>
          <div class="flex h-72 flex-col items-center justify-center gap-2">
            <IconifyIcon icon="lucide:heart-pulse" :size="32" class="text-gray-300" />
            <ElEmpty description="点击左侧开始检查 DB 与 Qdrant 一致性" />
          </div>
        </template>
      </ElCard>
    </div>
  </Page>
</template>
