<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalAgentStepLogApi } from '#/api/legal/agent-log';

import { Page } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getAgentStepLogPage } from '#/api/legal/agent-log';

import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'LegalAgentStepLog' });

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
          return await getAgentStepLogPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<LegalAgentStepLogApi.StepLog>,
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="Agent 调用日志" />
  </Page>
</template>
