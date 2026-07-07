<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalContractMemoryApi } from '#/api/legal/contract-memory';

import { confirm, useVbenModal } from '@vben/common-ui';

import { ElLoading, ElMessage } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteContractMemoryPage,
  getContractMemoryPage,
} from '#/api/legal/contract-memory';
import { $t } from '#/locales';

import { useMemoryGridColumns, useMemoryGridFormSchema } from '../data';
import MemoryForm from './memory-form.vue';

const [MemoryFormModal, memoryFormModalApi] = useVbenModal({
  connectedComponent: MemoryForm,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useMemoryGridFormSchema() },
  gridOptions: {
    columns: useMemoryGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          getContractMemoryPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<LegalContractMemoryApi.Memory>,
});

function refreshMemory() {
  gridApi.query();
}

function handleCreateMemory() {
  memoryFormModalApi.setData(null).open();
}

function handleEditMemory(row: LegalContractMemoryApi.Memory) {
  memoryFormModalApi.setData(row).open();
}

async function handleDeleteMemory(row: LegalContractMemoryApi.Memory) {
  if (!row.id || !row.contractId) {
    return;
  }
  await confirm($t('ui.actionMessage.deleteConfirm', [row.id]));
  const loading = ElLoading.service({ text: $t('ui.actionMessage.deleting', [row.id]) });
  try {
    await deleteContractMemoryPage(row.id, row.contractId);
    ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.id]));
    refreshMemory();
  } finally {
    loading.close();
  }
}
</script>

<template>
  <div class="h-full min-h-0">
    <Grid>
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create'),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              onClick: handleCreateMemory,
            },
          ]"
        />
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.edit'),
              type: 'primary',
              link: true,
              icon: ACTION_ICON.EDIT,
              onClick: () => handleEditMemory(row),
            },
            {
              label: $t('ui.actionTitle.delete'),
              type: 'danger',
              link: true,
              icon: ACTION_ICON.DELETE,
              onClick: () => handleDeleteMemory(row),
            },
          ]"
        />
      </template>
    </Grid>
    <MemoryFormModal @success="refreshMemory" />
  </div>
</template>
