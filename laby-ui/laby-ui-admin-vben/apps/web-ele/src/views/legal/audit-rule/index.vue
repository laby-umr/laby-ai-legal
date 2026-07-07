<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalAuditRuleApi } from '#/api/legal/audit-rule';

import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';

import { ElLoading, ElMessage, ElTag } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteAuditRule,
  deleteAuditRuleList,
  getAuditRule,
  getAuditRulePage,
  updateAuditRuleEnabled,
} from '#/api/legal/audit-rule';
import { $t } from '#/locales';

import { useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import LegalRulesConfigBanner from '../components/LegalRulesConfigBanner.vue';

defineOptions({ name: 'LegalAuditRule' });

const route = useRoute();

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

function handleRefresh() {
  gridApi.query();
}

function handleCreate() {
  formModalApi.setData(null).open();
}

function handleEdit(row: LegalAuditRuleApi.Rule) {
  formModalApi.setData(row).open();
}

async function handleToggleEnabled(row: LegalAuditRuleApi.Rule) {
  const enabled = !row.enabled;
  await updateAuditRuleEnabled(row.id!, enabled);
  ElMessage.success(enabled ? '已启用' : '已禁用');
  handleRefresh();
}

async function handleDelete(row: LegalAuditRuleApi.Rule) {
  const loadingInstance = ElLoading.service({
    text: $t('ui.actionMessage.deleting', [row.name]),
  });
  try {
    await deleteAuditRule(row.id!);
    ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.name]));
    handleRefresh();
  } finally {
    loadingInstance.close();
  }
}

async function handleDeleteBatch() {
  await confirm($t('ui.actionMessage.deleteBatchConfirm'));
  const loadingInstance = ElLoading.service({
    text: $t('ui.actionMessage.deletingBatch'),
  });
  try {
    await deleteAuditRuleList(checkedIds.value);
    checkedIds.value = [];
    ElMessage.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    loadingInstance.close();
  }
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({
  records,
}: {
  records: LegalAuditRuleApi.Rule[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema() },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    pagerConfig: { enabled: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          getAuditRulePage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<LegalAuditRuleApi.Rule>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});

onMounted(async () => {
  const rawId = route.query.id;
  if (rawId === undefined || rawId === null || rawId === '') {
    return;
  }
  const id = Number(rawId);
  if (!Number.isFinite(id)) {
    return;
  }
  try {
    const row = await getAuditRule(id);
    if (row) {
      handleEdit(row);
    }
  } catch {
    // 忽略无效深链
  }
});
</script>

<template>
  <Page auto-content-height>
    <LegalRulesConfigBanner />
    <FormModal @success="handleRefresh" />
    <Grid table-title="审核规则列表">
      <template #name="{ row }">
        <span>{{ row.name }}</span>
        <ElTag
          v-if="row.ruleType === 'PREFERRED_CLAUSE' && !row.standardClauseId"
          class="ml-1"
          size="small"
          type="warning"
        >
          待修复
        </ElTag>
      </template>
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['审核规则']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['legal:audit-rule:create'],
              onClick: handleCreate,
            },
            {
              label: $t('ui.actionTitle.deleteBatch'),
              type: 'danger',
              icon: ACTION_ICON.DELETE,
              disabled: isEmpty(checkedIds),
              auth: ['legal:audit-rule:delete'],
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: row.enabled ? '禁用' : '启用',
              type: 'primary',
              link: true,
              auth: ['legal:audit-rule:update'],
              onClick: handleToggleEnabled.bind(null, row),
            },
            {
              label: $t('common.edit'),
              type: 'primary',
              link: true,
              icon: ACTION_ICON.EDIT,
              auth: ['legal:audit-rule:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'danger',
              link: true,
              icon: ACTION_ICON.DELETE,
              auth: ['legal:audit-rule:delete'],
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
