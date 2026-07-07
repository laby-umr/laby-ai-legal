<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { AiKnowledgeDocumentApi } from '#/api/ai/knowledge/document';

import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictLabel } from '@vben/hooks';

import { ElLoading, ElMessage, ElProgress } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteKnowledgeDocument,
  getKnowledgeDocumentPage,
  retryKnowledgeDocumentIngest,
  updateKnowledgeDocumentStatus,
} from '#/api/ai/knowledge/document';
import { getKnowledgeSegmentProcessList } from '#/api/ai/knowledge/segment';
import { $t } from '#/locales';
import {
  canRetryIngest,
  IngestStatus,
} from '#/views/ai/knowledge/document/utils/ingest';
import {
  EMBEDDING_POLL_INTERVAL_MS,
  getProcessStatusLabel,
  isEmbeddingComplete,
  mergeProcessList,
  shouldPollProcess,
  type KnowledgeDocumentProcessState,
} from '#/views/ai/knowledge/document/utils/process';

import { useGridColumns, useGridFormSchema } from './data';
import CreateWizard from './modules/create-wizard.vue';

defineOptions({ name: 'AiKnowledgeDocument' });

const route = useRoute();
const router = useRouter();

const [CreateModal, createModalApi] = useVbenModal({
  connectedComponent: CreateWizard,
  destroyOnClose: true,
});

const progressMap = ref<Record<number, KnowledgeDocumentProcessState>>({});
const pollTimer = ref<null | number>(null);
const pageDocumentIds = ref<number[]>([]);

function getRowProgress(row: AiKnowledgeDocumentApi.KnowledgeDocument) {
  return progressMap.value[row.id!];
}

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  if (!route.query.knowledgeId) {
    ElMessage.error('知识库 ID 不存在');
    return;
  }
  createModalApi.setData({ knowledgeId: Number(route.query.knowledgeId) });
  createModalApi.open();
}

function handleCreateSuccess() {
  ElMessage.info('向量化在后台进行中，本列表将自动刷新进度');
  handleRefresh();
}

function handleEdit(id: number) {
  router.push({
    name: 'AiKnowledgeDocumentUpdate',
    query: { id, knowledgeId: route.query.knowledgeId },
  });
}

/** 查看向量化进度（可刷新后恢复） */
function handleViewProcess(row: AiKnowledgeDocumentApi.KnowledgeDocument) {
  router.push({
    name: 'AiKnowledgeDocumentUpdate',
    query: {
      id: row.id,
      knowledgeId: route.query.knowledgeId,
      step: 'process',
    },
  });
}

async function handleDelete(row: AiKnowledgeDocumentApi.KnowledgeDocument) {
  const loadingInstance = ElLoading.service({
    text: $t('ui.actionMessage.deleting', [row.name]),
  });
  try {
    await deleteKnowledgeDocument(row.id!);
    ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
    handleRefresh();
  } finally {
    loadingInstance.close();
  }
}

function handleSegment(id: number) {
  router.push({
    name: 'AiKnowledgeSegment',
    query: { documentId: id },
  });
}

function handleVectorHealth(docId?: number, knowledgeId?: string | number) {
  const query: Record<string, string | number> = {};
  if (docId) {
    query.documentId = docId;
  } else if (knowledgeId) {
    query.id = Number(knowledgeId);
  } else {
    ElMessage.error('知识库 ID 不存在');
    return;
  }
  router.push({
    name: 'AiKnowledgeVectorHealth',
    query,
  });
}

async function handleStatusChange(
  newStatus: number,
  row: AiKnowledgeDocumentApi.KnowledgeDocument,
): Promise<boolean | undefined> {
  try {
    await confirm(
      `你要将${row.name}的状态切换为【${getDictLabel(DICT_TYPE.COMMON_STATUS, newStatus)}】吗？`,
    );
  } catch {
    return false;
  }
  await updateKnowledgeDocumentStatus({
    id: row.id,
    status: newStatus,
  });
  ElMessage.success($t('ui.actionMessage.operationSuccess'));
  return true;
}

async function refreshEmbeddingProgress() {
  const ids = pageDocumentIds.value.filter(Boolean);
  if (ids.length === 0) {
    return;
  }
  try {
    const result = await getKnowledgeSegmentProcessList(ids);
    progressMap.value = mergeProcessList(result, progressMap.value);
  } catch (error) {
    console.error('刷新向量化进度失败', error);
  }
}

function schedulePoll() {
  if (pollTimer.value) {
    clearTimeout(pollTimer.value);
  }
  const needPoll = pageDocumentIds.value.some((id) =>
    shouldPollProcess(progressMap.value[id]),
  );
  if (!needPoll && pageDocumentIds.value.length === 0) {
    return;
  }
  if (needPoll || pageDocumentIds.value.some((id) => !progressMap.value[id])) {
    pollTimer.value = window.setTimeout(async () => {
      await refreshEmbeddingProgress();
      schedulePoll();
    }, EMBEDDING_POLL_INTERVAL_MS);
  }
}

async function handleRetryIngest(row: AiKnowledgeDocumentApi.KnowledgeDocument) {
  try {
    await confirm(`确认重新对「${row.name}」分段并向量化？将删除现有分段后重新处理。`);
    await retryKnowledgeDocumentIngest(row.id!);
    ElMessage.success('已提交重新入库，请稍候刷新进度');
    await refreshEmbeddingProgress();
    schedulePoll();
  } catch {
    // 用户取消
  }
}

function progressBarStatus(row: AiKnowledgeDocumentApi.KnowledgeDocument) {
  const s = getRowProgress(row);
  if (!s) {
    return undefined;
  }
  if (s.ingestStatus === IngestStatus.FAILED) {
    return 'exception';
  }
  if (isEmbeddingComplete(s.progress, s.count)) {
    return 'success';
  }
  if (s.stale) {
    return 'warning';
  }
  return undefined;
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(handleStatusChange),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const data = await getKnowledgeDocumentPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            knowledgeId: route.query.knowledgeId,
          });
          pageDocumentIds.value = (data.list || [])
            .map((item) => item.id)
            .filter((id): id is number => !!id);
          await refreshEmbeddingProgress();
          schedulePoll();
          return data;
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<AiKnowledgeDocumentApi.KnowledgeDocument>,
});

onMounted(() => {
  if (!route.query.knowledgeId) {
    ElMessage.error('知识库 ID 不存在，无法查看文档列表');
    router.back();
    return;
  }
  if (route.query.fromProcess === '1') {
    ElMessage.info('向量化在后台进行中，本列表将自动刷新进度');
  }
});

onBeforeUnmount(() => {
  if (pollTimer.value) {
    clearTimeout(pollTimer.value);
    pollTimer.value = null;
  }
});
</script>

<template>
  <Page auto-content-height>
    <CreateModal @success="handleCreateSuccess" />

    <Grid table-title="知识库文档列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '向量检查',
              type: 'default',
              icon: ACTION_ICON.VIEW,
              auth: ['ai:knowledge:update'],
              onClick: () =>
                handleVectorHealth(row.id, route.query.knowledgeId as string),
            },
            {
              label: $t('ui.actionTitle.create', ['知识库文档']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['ai:knowledge:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>

      <template #embeddingProgress="{ row }">
        <div v-if="row.id" class="py-1">
          <div class="mb-1 flex items-center justify-between text-xs text-gray-500">
            <span>{{ getProcessStatusLabel(getRowProgress(row)) }}</span>
            <span v-if="getRowProgress(row)?.count">
              {{ getRowProgress(row)?.embeddingCount || 0 }}/{{ getRowProgress(row)?.count }}
            </span>
          </div>
          <ElProgress
            :percentage="getRowProgress(row)?.progress ?? 0"
            :stroke-width="8"
            :status="progressBarStatus(row)"
          />
          <div
            v-if="getRowProgress(row)?.ingestError"
            class="mt-1 line-clamp-2 text-xs text-red-500"
            :title="getRowProgress(row)?.ingestError"
          >
            {{ getRowProgress(row)?.ingestError }}
          </div>
        </div>
        <span v-else class="text-gray-400">-</span>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '处理进度',
              type: 'primary',
              link: true,
              icon: 'lucide:loader',
              auth: ['ai:knowledge:query'],
              ifShow: () => {
                const s = getRowProgress(row);
                return !!s && shouldPollProcess(s);
              },
              onClick: handleViewProcess.bind(null, row),
            },
            {
              label: '重新入库',
              type: 'warning',
              link: true,
              icon: 'lucide:refresh-cw',
              auth: ['ai:knowledge:update'],
              ifShow: () => {
                const s = getRowProgress(row);
                return canRetryIngest(s?.ingestStatus, s?.stale);
              },
              onClick: handleRetryIngest.bind(null, row),
            },
            {
              label: $t('common.edit'),
              type: 'primary',
              link: true,
              icon: ACTION_ICON.EDIT,
              auth: ['ai:knowledge:update'],
              onClick: handleEdit.bind(null, row.id),
            },
            {
              label: '分段',
              type: 'primary',
              link: true,
              icon: ACTION_ICON.BOOK,
              auth: ['ai:knowledge:query'],
              onClick: handleSegment.bind(null, row.id),
            },
            {
              label: '向量检查',
              type: 'primary',
              link: true,
              icon: ACTION_ICON.VIEW,
              auth: ['ai:knowledge:update'],
              onClick: () =>
                handleVectorHealth(row.id, route.query.knowledgeId as string),
            },
            {
              label: $t('common.delete'),
              type: 'danger',
              link: true,
              icon: ACTION_ICON.DELETE,
              auth: ['ai:knowledge:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.name]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
