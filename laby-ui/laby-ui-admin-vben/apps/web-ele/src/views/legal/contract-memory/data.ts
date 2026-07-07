import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalContractMemoryApi } from '#/api/legal/contract-memory';

const MEMORY_TYPE_OPTIONS = [
  { label: '里程碑', value: 'milestone' },
  { label: '风险', value: 'risk' },
  { label: '决策', value: 'decision' },
  { label: 'Compaction 摘要', value: 'compaction_summary' },
  { label: '用户事实', value: 'fact' },
];

export function memoryTypeLabel(type?: string) {
  return MEMORY_TYPE_OPTIONS.find((item) => item.value === type)?.label ?? type ?? '-';
}

export function useMemoryGridFormSchema(): VbenFormSchema[] {
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
      fieldName: 'memoryType',
      label: '记忆类型',
      component: 'Select',
      componentProps: {
        options: MEMORY_TYPE_OPTIONS.filter((o) => o.value !== 'fact'),
        allowClear: true,
      },
    },
    {
      fieldName: 'content',
      label: '内容关键词',
      component: 'Input',
      componentProps: { placeholder: '模糊搜索' },
    },
  ];
}

export function useMemoryGridColumns(): VxeTableGridOptions<LegalContractMemoryApi.Memory>['columns'] {
  return [
    { field: 'id', title: '编号', minWidth: 80 },
    { field: 'contractId', title: '合同编号', minWidth: 100 },
    {
      field: 'memoryType',
      title: '类型',
      minWidth: 110,
      formatter: ({ cellValue }) => memoryTypeLabel(cellValue),
    },
    { field: 'content', title: '内容', minWidth: 280, showOverflow: 'tooltip' },
    { field: 'sessionId', title: '会话编号', minWidth: 160, showOverflow: 'tooltip' },
    { field: 'sourceMessageId', title: '来源消息', minWidth: 100 },
    { field: 'createTime', title: '创建时间', minWidth: 170, formatter: 'formatDateTime' },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

export function useUserFactGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'userId',
      label: '用户编号',
      component: 'InputNumber',
      componentProps: { placeholder: '用户 ID', controlsPosition: 'right', class: '!w-full' },
    },
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
      fieldName: 'content',
      label: '内容关键词',
      component: 'Input',
      componentProps: { placeholder: '模糊搜索' },
    },
  ];
}

export function useUserFactGridColumns(): VxeTableGridOptions<LegalContractMemoryApi.UserFact>['columns'] {
  return [
    { field: 'id', title: '编号', minWidth: 80 },
    { field: 'userId', title: '用户编号', minWidth: 100 },
    { field: 'contractId', title: '合同编号', minWidth: 100 },
    { field: 'content', title: '内容', minWidth: 280, showOverflow: 'tooltip' },
    { field: 'sessionId', title: '会话编号', minWidth: 160, showOverflow: 'tooltip' },
    { field: 'sourceMessageId', title: '来源消息', minWidth: 100 },
    { field: 'createTime', title: '创建时间', minWidth: 170, formatter: 'formatDateTime' },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}

export { MEMORY_TYPE_OPTIONS };
