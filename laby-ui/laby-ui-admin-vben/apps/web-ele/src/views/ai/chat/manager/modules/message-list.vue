<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { AiChatMessageApi } from '#/api/ai/chat/message';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { ElLoading, ElMessage } from 'element-plus';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteChatMessageByAdmin,
  getChatMessagePage,
} from '#/api/ai/chat/message';
import { $t } from '#/locales';
import RecallDiagnosticsPanel from '#/views/ai/chat/components/recall-diagnostics-panel.vue';
import { hasRecallDiagnostics } from '#/views/ai/chat/components/recall-diagnostics-utils';

import { useGridColumnsMessage, useGridFormSchemaMessage } from '../data';

const [Drawer, drawerApi] = useVbenDrawer({
  title: '召回诊断详情',
  footer: false,
  destroyOnClose: true,
});

/** 刷新表格 */
function handleRefresh() {
  gridApi.query();
}

/** 查看召回诊断 */
function openRecallDiagnostics(row: AiChatMessageApi.ChatMessage) {
  drawerApi.setData(row).open();
}

/** 删除消息 */
async function handleDelete(row: AiChatMessageApi.ChatMessage) {
  const loadingInstance = ElLoading.service({
    text: $t('ui.actionMessage.deleting', [row.id]),
  });
  try {
    await deleteChatMessageByAdmin(row.id!);
    ElMessage.success($t('ui.actionMessage.deleteSuccess', [row.id]));
    handleRefresh();
  } finally {
    loadingInstance.close();
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchemaMessage(),
  },
  gridOptions: {
    columns: useGridColumnsMessage(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getChatMessagePage({
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
  } as VxeTableGridOptions<AiChatMessageApi.ChatMessage>,
});
</script>

<template>
  <Page auto-content-height>
    <Drawer class="w-[560px]">
      <template v-if="drawerApi.getData()">
        <div class="mb-4 text-sm text-gray-500">
          消息 #{{ (drawerApi.getData() as AiChatMessageApi.ChatMessage).id }}
        </div>
        <RecallDiagnosticsPanel
          always-expanded
          :recall-diagnostics="
            (drawerApi.getData() as AiChatMessageApi.ChatMessage)
              .recallDiagnostics
          "
        />
      </template>
    </Drawer>

    <Grid table-title="消息列表">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '召回诊断',
              type: 'primary',
              link: true,
              icon: ACTION_ICON.VIEW,
              ifShow: hasRecallDiagnostics(row),
              onClick: openRecallDiagnostics.bind(null, row),
            },
            {
              label: $t('common.delete'),
              type: 'danger',
              link: true,
              icon: ACTION_ICON.DELETE,
              auth: ['ai:chat-message:delete'],
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
