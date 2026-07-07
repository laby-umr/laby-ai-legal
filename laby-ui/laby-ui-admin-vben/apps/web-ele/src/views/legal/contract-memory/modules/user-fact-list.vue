<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalContractMemoryApi } from '#/api/legal/contract-memory';

import { confirm, useVbenModal } from '@vben/common-ui';

import { ElLoading, ElMessage } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteUserFact, getUserFactPage } from '#/api/legal/contract-memory';
import { $t } from '#/locales';

import { useUserFactGridColumns, useUserFactGridFormSchema } from '../data';
import UserFactForm from './user-fact-form.vue';

const [UserFactFormModal, userFactFormModalApi] = useVbenModal({
  connectedComponent: UserFactForm,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useUserFactGridFormSchema() },
  gridOptions: {
    columns: useUserFactGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          getUserFactPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<LegalContractMemoryApi.UserFact>,
});

function refreshUserFact() {
  gridApi.query();
}

function handleCreateUserFact() {
  userFactFormModalApi.setData(null).open();
}

function handleEditUserFact(row: LegalContractMemoryApi.UserFact) {
  userFactFormModalApi.setData(row).open();
}

async function handleDeleteUserFact(row: LegalContractMemoryApi.UserFact) {
  if (!row.id) {
    return;
  }
  await confirm($t('ui.actionMessage.deleteConfirm', [row.id]));
  const loading = ElLoading.service({ text: $t('ui.actionMessage.deleting', [row.id]) });
  try {
    await deleteUserFact(row.id);
    ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.id]));
    refreshUserFact();
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
              onClick: handleCreateUserFact,
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
              onClick: () => handleEditUserFact(row),
            },
            {
              label: $t('ui.actionTitle.delete'),
              type: 'danger',
              link: true,
              icon: ACTION_ICON.DELETE,
              onClick: () => handleDeleteUserFact(row),
            },
          ]"
        />
      </template>
    </Grid>
    <UserFactFormModal @success="refreshUserFact" />
  </div>
</template>
