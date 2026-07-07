import type { VbenFormSchema } from '#/adapter/form';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';



import { DICT_TYPE } from '@vben/constants';

import { getDictOptions } from '@vben/hooks';



import { getRangePickerDefaultProps } from '#/utils';



/** 列表检索 */

export function useGridFormSchema(): VbenFormSchema[] {

  return [

    {

      fieldName: 'title',

      label: '合同标题',

      component: 'Input',

      componentProps: {

        placeholder: '请输入合同标题',

        clearable: true,

      },

    },

    {

      fieldName: 'status',

      label: '业务状态',

      component: 'Select',

      componentProps: {

        options: getDictOptions(DICT_TYPE.LEGAL_CONTRACT_STATUS, 'number'),

        placeholder: '请选择状态',

        clearable: true,

      },

    },

    {

      fieldName: 'partyRole',

      label: '我方立场',

      component: 'Select',

      componentProps: {

        options: getDictOptions(DICT_TYPE.LEGAL_PARTY_ROLE, 'string'),

        placeholder: '请选择立场',

        clearable: true,

      },

    },

    {

      fieldName: 'createTime',

      label: '创建时间',

      component: 'RangePicker',

      componentProps: {

        ...getRangePickerDefaultProps(),

        clearable: true,

      },

    },

  ];

}



/** 列表列 */

export function useGridColumns(): VxeTableGridOptions['columns'] {

  return [

    {

      field: 'id',

      title: '编号',

      minWidth: 80,

    },

    {

      field: 'title',

      title: '合同标题',

      minWidth: 200,

    },

    {

      field: 'statusName',

      title: '业务状态',

      minWidth: 120,

      slots: { default: 'status' },

    },

    {

      field: 'partyRole',

      title: '立场',

      minWidth: 80,

      cellRender: {

        name: 'CellDict',

        props: { type: DICT_TYPE.LEGAL_PARTY_ROLE },

      },

    },

    {

      field: 'auditOpinionCount',

      title: 'AI 意见',

      minWidth: 90,

      align: 'center',

    },

    {

      field: 'hasAuditReport',

      title: 'AI 报告',

      minWidth: 100,

      align: 'center',

      slots: { default: 'auditReport' },

    },

    {

      field: 'riskHighCount',

      title: '高风险',

      minWidth: 80,

      align: 'center',

    },

    {

      field: 'createTime',

      title: '创建时间',

      minWidth: 180,

      formatter: 'formatDateTime',

    },

    {

      field: 'failReason',

      title: '说明',

      minWidth: 160,

      showOverflow: 'tooltip',

      slots: { default: 'remark' },

    },

    {

      title: '操作',

      width: 220,

      fixed: 'right',

      slots: { default: 'actions' },

    },

  ];

}

