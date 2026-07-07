<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalContractApi } from '#/api/legal/contract';

import { useRouter } from 'vue-router';

import { DocAlert, Page, useVbenModal } from '@vben/common-ui';

import { ElMessage, ElTag } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getContractPage, retryContractPipeline } from '#/api/legal/contract';

import { CONTRACT_STATUS } from './constants';
import { useGridColumns, useGridFormSchema } from './data';
import CreateForm from './modules/create-form.vue';

defineOptions({ name: 'LegalContractList' });

const STATUS = CONTRACT_STATUS;
const router = useRouter();

const [CreateModal, createModalApi] = useVbenModal({
  connectedComponent: CreateForm,
  destroyOnClose: true,
});

function statusTagType(row: LegalContractApi.Contract) {
  if (row.status === STATUS.FAILED) {
    return 'danger';
  }
  if (row.status === STATUS.PARSING || row.status === STATUS.AI_AUDITING) {
    return 'warning';
  }
  if (row.status === STATUS.OPINION_REVIEW) {
    return 'success';
  }
  if (row.status === STATUS.ARCHIVED) {
    return 'info';
  }
  return 'primary';
}

function openReview(row: LegalContractApi.Contract) {
  if (row.processInstanceId) {
    router.push({
      name: 'BpmProcessInstanceDetail',
      query: { id: row.processInstanceId },
    });
    return;
  }
  router.push({
    name: 'LegalContractReview',
    query: { id: String(row.id) },
  });
}

function openDetail(row: LegalContractApi.Contract) {
  router.push({
    name: 'LegalContractDetail',
    query: { id: String(row.id) },
  });
}

function handleCreate() {
  createModalApi.setData(null).open();
}

function handleCreateSuccess() {
  gridApi.query();
}

async function handleRetry(row: LegalContractApi.Contract) {
  try {
    await retryContractPipeline(row.id);
    ElMessage.success('已重新提交后台处理');
    gridApi.query();
  } catch {
    /* 全局错误提示 */
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getContractPage({
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
  } as VxeTableGridOptions<LegalContractApi.Contract>,
});
</script>

<template>
  <Page auto-content-height>
    <CreateModal @success="handleCreateSuccess" />

    <template #doc>
      <DocAlert
        title="法务合同 AI 审核"
        url="https://doc.iocoder.cn/"
      />
    </template>

    <Grid table-title="我的合同">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: '新建合同审核',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['legal:contract:create'],
              onClick: handleCreate,
            },
          ]"
        />
      </template>

      <template #status="{ row }">
        <ElTag :type="statusTagType(row)" size="small">
          {{ row.statusName || row.status }}
        </ElTag>
      </template>

      <template #auditReport="{ row }">
        <ElTag v-if="row.hasAuditReport" type="success" size="small">
          已生成
        </ElTag>
        <ElTag
          v-else-if="
            row.status === STATUS.PARSING || row.status === STATUS.AI_AUDITING
          "
          type="warning"
          size="small"
        >
          生成中
        </ElTag>
        <ElTag v-else-if="row.status === STATUS.FAILED" type="danger" size="small">
          无
        </ElTag>
        <ElTag v-else type="info" size="small">无</ElTag>
      </template>

      <template #remark="{ row }">
        <span
          v-if="row.status === STATUS.FAILED && row.failReason"
          class="text-destructive"
        >
          {{ row.failReason }}
        </span>
        <span
          v-else-if="
            row.status === STATUS.PARSING || row.status === STATUS.AI_AUDITING
          "
          class="text-muted-foreground"
        >
          后台处理中
        </span>
        <span v-else>-</span>
      </template>

      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '详情',
              type: 'primary',
              link: true,
              icon: ACTION_ICON.VIEW,
              auth: ['legal:contract:query'],
              onClick: () => openDetail(row),
            },
            {
              label: row.processInstanceId ? '办理' : '审核',
              type: 'primary',
              link: true,
              icon: ACTION_ICON.EDIT,
              ifShow: row.reviewActionVisible,
              auth: ['legal:contract:update'],
              onClick: () => openReview(row),
            },
            {
              label: '重试',
              type: 'danger',
              link: true,
              icon: ACTION_ICON.REFRESH,
              ifShow: row.retryVisible,
              auth: ['legal:contract:create'],
              onClick: () => handleRetry(row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
