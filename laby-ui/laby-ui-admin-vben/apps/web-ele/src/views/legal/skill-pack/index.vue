<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalSkillPackApi } from '#/api/legal/skill-pack';

import { onMounted, ref } from 'vue';

import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { isEmpty } from '@vben/utils';

import { ElLoading, ElMessage } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  copySkillPack,
  deleteSkillPack,
  deleteSkillPackList,
  getSkillPackPage,
  updateSkillPackEnabled,
} from '#/api/legal/skill-pack';
import { $t } from '#/locales';

import { fetchLegalChatRoles, useGridColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import LegalRulesConfigBanner from '../components/LegalRulesConfigBanner.vue';

defineOptions({ name: 'LegalSkillPack' });

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

function handleEdit(row: LegalSkillPackApi.SkillPack) {
  formModalApi.setData(row).open();
}

async function handleToggleEnabled(row: LegalSkillPackApi.SkillPack) {
  const enabled = !row.enabled;
  await updateSkillPackEnabled(row.id!, enabled);
  ElMessage.success(enabled ? '已启用' : '已禁用');
  handleRefresh();
}

async function handleCopy(row: LegalSkillPackApi.SkillPack) {
  await copySkillPack(row.id!);
  ElMessage.success('已复制技能包');
  handleRefresh();
}

async function handleDelete(row: LegalSkillPackApi.SkillPack) {
  const loadingInstance = ElLoading.service({
    text: $t('ui.actionMessage.deleting', [row.name]),
  });
  try {
    await deleteSkillPack(row.id!);
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
    await deleteSkillPackList(checkedIds.value);
    checkedIds.value = [];
    ElMessage.success($t('ui.actionMessage.deleteSuccess'));
    handleRefresh();
  } finally {
    loadingInstance.close();
  }
}

const chatRoleNameMap = ref<Record<number, string>>({});

onMounted(async () => {
  try {
    const roles = await fetchLegalChatRoles();
    chatRoleNameMap.value = Object.fromEntries(
      roles.map((role) => [role.id, role.name]),
    );
  } catch {
    chatRoleNameMap.value = {};
  }
});

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({
  records,
}: {
  records: LegalSkillPackApi.SkillPack[];
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
          getSkillPackPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<LegalSkillPackApi.SkillPack>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <LegalRulesConfigBanner />
    <FormModal @success="handleRefresh" />
    <Grid table-title="AI 技能包">
      <template #toolbar-tools>
        <TableAction
          :actions="[
            {
              label: $t('ui.actionTitle.create', ['AI 技能包']),
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: ['legal:skill-pack:create'],
              onClick: handleCreate,
            },
            {
              label: $t('ui.actionTitle.deleteBatch'),
              type: 'danger',
              icon: ACTION_ICON.DELETE,
              disabled: isEmpty(checkedIds),
              auth: ['legal:skill-pack:delete'],
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>
      <template #chatRole="{ row }">
        <span v-if="row.chatRoleId">
          {{ chatRoleNameMap[row.chatRoleId] || `角色 #${row.chatRoleId}` }}
        </span>
        <span v-else class="text-gray-400">—</span>
      </template>
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: row.enabled ? '禁用' : '启用',
              type: 'primary',
              link: true,
              auth: ['legal:skill-pack:update'],
              onClick: handleToggleEnabled.bind(null, row),
            },
            {
              label: '复制',
              type: 'primary',
              link: true,
              auth: ['legal:skill-pack:create'],
              onClick: handleCopy.bind(null, row),
            },
            {
              label: $t('common.edit'),
              type: 'primary',
              link: true,
              icon: ACTION_ICON.EDIT,
              auth: ['legal:skill-pack:update'],
              onClick: handleEdit.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'danger',
              link: true,
              icon: ACTION_ICON.DELETE,
              auth: ['legal:skill-pack:delete'],
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
