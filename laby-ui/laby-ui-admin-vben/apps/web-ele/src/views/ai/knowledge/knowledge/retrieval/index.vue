<script setup lang="ts">
import type {
  KnowledgeSegmentSearchResult,
  RecallDiagnostics,
} from '#/api/ai/knowledge/segment';

import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  ElButton,
  ElCard,
  ElEmpty,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElTag,
} from 'element-plus';

import { getKnowledge } from '#/api/ai/knowledge/knowledge';
import { searchKnowledgeSegment } from '#/api/ai/knowledge/segment';

/** 知识库文档召回测试 */
defineOptions({ name: 'KnowledgeDocumentRetrieval' });

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const segments = ref<any[]>([]);
const diagnostics = ref<RecallDiagnostics | null>(null);
const queryParams = reactive({
  id: undefined,
  content: '',
  topK: 8,
  similarityThreshold: 0.6,
});

function normalizeSearchResult(
  data: KnowledgeSegmentSearchResult | any[] | null | undefined,
) {
  if (!data) {
    return { segments: [], diagnostics: null };
  }
  if (Array.isArray(data)) {
    return { segments: data, diagnostics: null };
  }
  return {
    segments: data.segments ?? [],
    diagnostics: data.diagnostics ?? null,
  };
}

/** 执行召回测试 */
async function getRetrievalResult() {
  if (!queryParams.content) {
    ElMessage.warning('请输入查询文本');
    return;
  }

  loading.value = true;
  segments.value = [];
  diagnostics.value = null;

  try {
    const data = await searchKnowledgeSegment({
      knowledgeId: queryParams.id,
      content: queryParams.content,
      topK: queryParams.topK,
      similarityThreshold: queryParams.similarityThreshold,
    });
    const normalized = normalizeSearchResult(data);
    segments.value = normalized.segments;
    diagnostics.value = normalized.diagnostics;
  } finally {
    loading.value = false;
  }
}

/** 切换段落展开状态 */
function toggleExpand(segment: any) {
  segment.expanded = !segment.expanded;
}

/** 获取知识库配置信息 */
async function getKnowledgeInfo(id: number) {
  try {
    const knowledge = await getKnowledge(id);
    if (knowledge) {
      queryParams.topK = knowledge.topK ?? 8;
      queryParams.similarityThreshold = knowledge.similarityThreshold ?? 0.6;
    }
  } catch {}
}

/** 初始化 */
onMounted(() => {
  if (!route.query.id) {
    ElMessage.error('知识库 ID 不存在，无法进行召回测试');
    router.back();
    return;
  }
  queryParams.id = route.query.id as any;
  getKnowledgeInfo(queryParams.id as any);
});
</script>
<template>
  <Page auto-content-height>
    <div class="flex w-full gap-4">
      <ElCard class="w-3/4 flex-1">
        <div class="mb-15">
          <h3 class="m-2 text-lg font-semibold leading-none tracking-tight">
            召回测试
          </h3>
          <div class="m-2 text-sm text-gray-500">
            根据给定的查询文本测试召回效果。
          </div>
        </div>
        <div>
          <div class="relative m-2">
            <ElInput
              v-model="queryParams.content"
              :rows="8"
              type="textarea"
              placeholder="请输入文本"
            />
            <div class="absolute bottom-2 right-2 text-sm text-gray-400">
              {{ queryParams.content?.length }} / 200
            </div>
          </div>
          <div class="m-2 flex items-center">
            <span class="w-16 text-gray-500">topK:</span>
            <ElInputNumber
              v-model="queryParams.topK"
              :min="1"
              :max="20"
              controls-position="right"
              class="!w-full"
            />
          </div>
          <div class="m-2 flex items-center">
            <span class="w-16 text-gray-500">相似度:</span>
            <ElInputNumber
              v-model="queryParams.similarityThreshold"
              class="!w-full"
              controls-position="right"
              :min="0"
              :max="1"
              :precision="2"
              :step="0.01"
            />
          </div>
          <div class="flex justify-end">
            <ElButton
              type="primary"
              @click="getRetrievalResult"
              :loading="loading"
            >
              测试
            </ElButton>
          </div>
        </div>
      </ElCard>
      <ElCard class="min-w-300 flex-1">
        <div
          v-if="diagnostics"
          class="mb-4 rounded border border-solid border-gray-200 bg-gray-50 p-3 text-sm"
        >
          <div class="mb-2 font-semibold text-gray-700">召回诊断</div>
          <div v-if="diagnostics.intent" class="mb-1 text-gray-600">
            意图：
            <ElTag size="small" type="info">{{ diagnostics.intent }}</ElTag>
          </div>
          <div
            v-if="diagnostics.queryVariants?.length"
            class="mb-1 text-gray-600"
          >
            查询变体：
            <ElTag
              v-for="(variant, idx) in diagnostics.queryVariants"
              :key="idx"
              size="small"
              class="mr-1"
            >
              {{ variant }}
            </ElTag>
          </div>
          <div class="flex flex-wrap gap-3 text-xs text-gray-500">
            <span v-if="diagnostics.denseHitCount != null">
              Dense: {{ diagnostics.denseHitCount }}
            </span>
            <span v-if="diagnostics.sparseHitCount != null">
              Sparse: {{ diagnostics.sparseHitCount }}
            </span>
            <span v-if="diagnostics.fusedHitCount != null">
              融合: {{ diagnostics.fusedHitCount }}
            </span>
            <span v-if="diagnostics.rerankHitCount != null">
              Rerank: {{ diagnostics.rerankHitCount }}
            </span>
            <span v-if="diagnostics.topScore != null">
              Top1: {{ diagnostics.topScore }}
            </span>
            <span v-if="diagnostics.latencyMs != null">
              {{ diagnostics.latencyMs }}ms
            </span>
          </div>
          <div
            v-if="diagnostics.notes?.length"
            class="mt-2 space-y-1 text-xs text-amber-700"
          >
            <div v-for="(note, idx) in diagnostics.notes" :key="idx">
              {{ note }}
            </div>
          </div>
        </div>

        <template v-if="loading">
          <div class="flex h-72 items-center justify-center">
            <ElEmpty description="正在检索中..." />
          </div>
        </template>

        <template v-else-if="segments.length > 0">
          <div class="mb-15 font-bold">{{ segments.length }} 个召回段落</div>
          <div>
            <div
              v-for="(segment, index) in segments"
              :key="index"
              class="mt-2 rounded border border-solid border-gray-200 px-2 py-2"
            >
              <div
                class="mb-2 flex items-center justify-between gap-8 text-sm text-gray-500"
              >
                <span>
                  分段({{ segment.id }}) · {{ segment.contentLength }} 字符数 ·
                  {{ segment.tokens }} Token
                </span>
                <span
                  class="whitespace-nowrap rounded-full bg-blue-50 px-2 py-1 text-sm text-blue-500"
                >
                  score: {{ segment.score }}
                </span>
              </div>
              <div
                class="mb-2 overflow-hidden whitespace-pre-wrap rounded bg-gray-50 text-sm transition-all duration-100"
                :class="{
                  'line-clamp-2 max-h-40': !segment.expanded,
                  'max-h-[1500px]': segment.expanded,
                }"
              >
                {{ segment.content }}
              </div>
              <div class="flex items-center justify-between gap-8">
                <div class="flex items-center gap-1 text-sm text-gray-500">
                  <IconifyIcon icon="lucide:file-text" />
                  <span>{{ segment.documentName || '未知文档' }}</span>
                </div>
                <ElButton size="small" @click="toggleExpand(segment)">
                  {{ segment.expanded ? '收起' : '展开' }}
                  <IconifyIcon
                    :icon="
                      segment.expanded
                        ? 'lucide:chevron-up'
                        : 'lucide:chevron-down'
                    "
                  />
                </ElButton>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="flex h-72 items-center justify-center">
            <ElEmpty description="暂无召回结果" />
          </div>
        </template>
      </ElCard>
    </div>
  </Page>
</template>
