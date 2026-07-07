import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalStandardClauseApi } from '#/api/legal/standard-clause';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

/** 新增/修改表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'name',
      label: '条款名称',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入条款名称' },
    },
    {
      fieldName: 'clauseType',
      label: '条款分类',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_CLAUSE_TYPE),
        placeholder: '请选择或输入',
        allowCreate: true,
        filterable: true,
        defaultFirstOption: true,
      },
    },
    {
      fieldName: 'categoryScope',
      label: '适用范围',
      component: 'Select',
      defaultValue: 'COMMON',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_STANDARD_CLAUSE_SCOPE),
        placeholder: '请选择',
      },
    },
    {
      fieldName: 'content',
      label: '条款正文',
      rules: 'required',
      formItemClass: 'col-span-2',
      component: 'Textarea',
      componentProps: { rows: 6, placeholder: '标准条款全文' },
    },
    {
      fieldName: 'referenceSource',
      label: '来源说明',
      component: 'Input',
      componentProps: { placeholder: '模板版本、制度编号等' },
    },
    {
      fieldName: 'sort',
      label: '排序',
      component: 'InputNumber',
      defaultValue: 0,
      componentProps: { min: 0, class: '!w-full' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 0,
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
      },
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'name',
      label: '条款名称',
      component: 'Input',
      componentProps: { placeholder: '请输入', clearable: true },
    },
    {
      fieldName: 'clauseType',
      label: '条款分类',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_CLAUSE_TYPE),
        clearable: true,
        placeholder: '请选择',
        filterable: true,
      },
    },
    {
      fieldName: 'categoryScope',
      label: '适用范围',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_STANDARD_CLAUSE_SCOPE),
        clearable: true,
        placeholder: '请选择',
      },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        clearable: true,
        placeholder: '请选择',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<LegalStandardClauseApi.Clause>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'id', title: '编号', minWidth: 80 },
    { field: 'name', title: '条款名称', minWidth: 160 },
    {
      field: 'clauseType',
      title: '分类',
      width: 100,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.LEGAL_CLAUSE_TYPE },
      },
    },
    {
      field: 'categoryScope',
      title: '范围',
      width: 90,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.LEGAL_STANDARD_CLAUSE_SCOPE },
      },
    },
    { field: 'referenceSource', title: '来源', minWidth: 120 },
    { field: 'sort', title: '排序', width: 80 },
    {
      field: 'status',
      title: '状态',
      width: 90,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.COMMON_STATUS },
      },
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 170,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 160,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
