import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalAgentStepLogApi } from '#/api/legal/agent-log';

/** 列表搜索 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'contractId',
      label: '合同编号',
      component: 'InputNumber',
      componentProps: { placeholder: '合同 ID', controlsPosition: 'right', class: '!w-full' },
    },
    {
      fieldName: 'sessionId',
      label: '会话编号',
      component: 'Input',
      componentProps: { placeholder: 'sessionId' },
    },
    {
      fieldName: 'stepType',
      label: '步骤类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: 'LLM', value: 'LLM' },
          { label: 'TOOL', value: 'TOOL' },
          { label: 'ERROR', value: 'ERROR' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'toolName',
      label: 'Tool 名称',
      component: 'Input',
      componentProps: { placeholder: 'legal_search_paragraphs' },
    },
  ];
}

/** 列表字段 */
export function useGridColumns(): VxeTableGridOptions<LegalAgentStepLogApi.StepLog>['columns'] {
  return [
    { field: 'id', title: '编号', minWidth: 80 },
    { field: 'contractId', title: '合同编号', minWidth: 100 },
    { field: 'sessionId', title: '会话编号', minWidth: 160, showOverflow: true },
    { field: 'stepIndex', title: '步骤', minWidth: 70 },
    { field: 'stepType', title: '类型', minWidth: 80 },
    { field: 'toolName', title: 'Tool', minWidth: 180, showOverflow: true },
    { field: 'toolOutputSummary', title: '摘要', minWidth: 220, showOverflow: true },
    { field: 'latencyMs', title: '耗时(ms)', minWidth: 90 },
    { field: 'createTime', title: '时间', minWidth: 170, formatter: 'formatDateTime' },
  ];
}
