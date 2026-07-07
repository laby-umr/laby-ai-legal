<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { AiKnowledgeSegmentApi } from '#/api/ai/knowledge/segment';

import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictLabel } from '@vben/hooks';

import { ElLoading, ElMessage } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteKnowledgeSegment,
  getKnowledgeSegmentPage,
  updateKnowledgeSegmentStatus,
} from '#/api/ai/knowledge/segment';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const route = useRoute();
const router = useRouter();

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 创建知识库片段 */
function handleCreate() {
  formModalApi.setData({ documentId: route.query.documentId }).open();
}

/** 编辑知识库片段 */
function handleEdit(row: AiKnowledgeSegmentApi.KnowledgeSegment) {
  formModalApi.setData(row).open();
}

/** 删除知识库片段 */
async function handleDelete(row: AiKnowledgeSegmentApi.KnowledgeSegment) {
  const loadingInstance = ElLoading.service({
    text: $t('ui.actionMessage.deleting', [row.id]),
  });
  try {
    await deleteKnowledgeSegment(row.id!);
    ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.id]));
    handleRefresh();
  } finally {
    loadingInstance.close();
  }
}

/** 更新知识库片段状态 */
async function handleStatusChange(
  newStatus: number,
  row: AiKnowledgeSegmentApi.KnowledgeSegment,
): Promise<boolean | undefined> {
  try {
    await confirm(`你要将片段 ${row.id} 的状态切换为【${getDictLabel(DICT_TYPE.COMMON_STATUS, newStatus)}】吗？`);
  } catch {
    return false;
  }
  // 更新片段状态
  await updateKnowledgeSegmentStatus(row.id!, newStatus);
  // 提示并返回成功
  ElMessage.success($t('ui.actionMessage.operationSuccess'));
  return true;
}

function handleVectorHealth() {
  const documentId = route.query.documentId;
  if (!documentId) {
    ElMessage.error('文档编号不存在');
    return;
  }
  router.push({
    name: 'AiKnowledgeVectorHealth',
    query: { documentId },
  });
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
          return await getKnowledgeSegmentPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
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
  } as VxeTableGridOptions<AiKnowledgeSegmentApi.KnowledgeSegment>,
});

/** 初始化 */
onMounted(() => {
  gridApi.formApi.setFieldValue('documentId', route.query.documentId);
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Grid table-title="分段列表">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '向量检查',
              type: 'default',
              icon: ACTION_ICON.VIEW,
              auth: ['ai:knowledge:update'],
              onClick: handleVectorHealth,
            },
            {
              label: $t('ui.actionTitle.create', ['分段']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['ai:knowledge:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>
      <template #expand_content="{ row }">
        <div class="space-y-3 border-l-4 border-blue-500 px-2.5 py-5 text-sm leading-5">
          <div>
            <div class="mb-1 font-bold text-gray-600">完整内容</div>
            <div class="whitespace-pre-wrap text-gray-700">{{ row.content }}</div>
          </div>
          <div v-if="row.embedText && row.embedText !== row.content">
            <div class="mb-1 font-bold text-gray-600">Embed 文本</div>
            <div class="whitespace-pre-wrap text-gray-600">{{ row.embedText }}</div>
          </div>
          <div>
            <div class="mb-1 font-bold text-gray-600">
              Sparse 文本
              <span
                v-if="!row.sparseText"
                class="ml-1 text-xs font-normal text-amber-600"
              >
                （未回填，Hybrid 检索可能降级）
              </span>
            </div>
            <div class="whitespace-pre-wrap text-gray-600">
              {{ row.sparseText || '-' }}
            </div>
          </div>
        </div>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('common.edit'),
              type: 'primary',
              link: true,
              icon: ACTION_ICON.EDIT,
              auth: ['ai:knowledge:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'danger',
              link: true,
              icon: ACTION_ICON.DELETE,
              auth: ['ai:knowledge:delete'],
              popConfirm: {
                title: $t('ui.actionMessage.deleteConfirm', [row.id]),
                confirm: handleDelete.bind(null, row),
              },
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
