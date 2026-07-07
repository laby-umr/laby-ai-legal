import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalAiTraceApi } from '#/api/legal/ai-trace';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'contractId',
      label: '合同 ID',
      component: 'InputNumber',
      componentProps: { min: 1, controlsPosition: 'right', class: '!w-full' },
    },
    {
      fieldName: 'scene',
      label: '场景',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_SKILL_PACK_SCENE, 'string'),
        clearable: true,
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_AI_TRACE_STATUS, 'string'),
        clearable: true,
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<LegalAiTraceApi.Trace>['columns'] {
  return [
    { field: 'traceId', title: 'Trace ID', minWidth: 200 },
    { field: 'contractId', title: '合同 ID', width: 100 },
    {
      field: 'scene',
      title: '场景',
      width: 110,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.LEGAL_SKILL_PACK_SCENE },
      },
    },
    { field: 'auditRound', title: '轮次', width: 70 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.LEGAL_AI_TRACE_STATUS },
      },
    },
    { field: 'deterministicCount', title: 'Playbook', width: 90 },
    { field: 'opinionCount', title: '意见数', width: 80 },
    { field: 'latencyMs', title: '耗时(ms)', width: 100 },
    {
      field: 'createTime',
      title: '时间',
      minWidth: 170,
      formatter: 'formatDateTime',
    },
    { field: 'errorMessage', title: '失败原因', minWidth: 160 },
  ];
}
