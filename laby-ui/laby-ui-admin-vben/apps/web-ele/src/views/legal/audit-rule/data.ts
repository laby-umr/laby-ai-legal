import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalAuditRuleApi } from '#/api/legal/audit-rule';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { getContractTypeSimpleList } from '#/api/legal/contract-type';
import { getStandardClauseSimpleList } from '#/api/legal/standard-clause';

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
      label: '规则名称',
      rules: 'required',
      formItemClass: 'col-span-2',
      component: 'Input',
      componentProps: { placeholder: '请输入规则名称' },
    },
    {
      fieldName: 'contractTypeId',
      label: '合同类型',
      component: 'ApiSelect',
      componentProps: {
        api: () =>
          getContractTypeSimpleList().then((list) =>
            (list || []).map((item) => ({ label: item.name, value: item.id })),
          ),
        placeholder: '空表示适用全部类型',
        allowClear: true,
      },
    },
    {
      fieldName: 'clauseType',
      label: '条款分类',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_CLAUSE_TYPE),
        placeholder: '可选',
        allowClear: true,
        filterable: true,
      },
    },
    {
      fieldName: 'standardClauseId',
      label: '标准条款',
      component: 'ApiSelect',
      componentProps: {
        api: () =>
          getStandardClauseSimpleList().then((list) =>
            (list || []).map((item) => ({ label: item.name, value: item.id })),
          ),
        placeholder: '推荐标准条款类型必选',
        allowClear: true,
      },
      dependencies: {
        triggerFields: ['ruleType'],
        rules(values) {
          return values.ruleType === 'PREFERRED_CLAUSE' ? 'required' : null;
        },
      },
    },
    {
      fieldName: 'ruleType',
      label: '规则类型',
      component: 'Select',
      defaultValue: 'CUSTOM_LLM',
      componentProps: {
        options: [
          { label: '必备条款', value: 'MANDATORY_CLAUSE' },
          { label: '禁止表述', value: 'FORBIDDEN_PATTERN' },
          { label: '推荐标准条款', value: 'PREFERRED_CLAUSE' },
          { label: 'LLM 补充', value: 'CUSTOM_LLM' },
        ],
      },
    },
    {
      fieldName: 'matchPattern',
      label: '匹配关键词/正则',
      component: 'Input',
      componentProps: { placeholder: '必备/禁止/推荐检测用' },
    },
    {
      fieldName: 'matchType',
      label: '匹配方式',
      component: 'Select',
      defaultValue: 'KEYWORD',
      componentProps: {
        options: [
          { label: '关键词', value: 'KEYWORD' },
          { label: '正则', value: 'REGEX' },
        ],
      },
    },
    {
      fieldName: 'riskLevel',
      label: '风险等级',
      component: 'Select',
      defaultValue: 'HIGH',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_RISK_LEVEL, 'string'),
      },
    },
    {
      fieldName: 'ruleContent',
      label: '规则正文',
      formItemClass: 'col-span-2',
      component: 'Textarea',
      help: '写检查要求或 LLM 补充清单；勿粘贴标准条款全文，推荐条款请选上方标准条款',
      componentProps: { rows: 4, placeholder: '审核要求说明' },
    },
    {
      fieldName: 'description',
      label: '说明',
      formItemClass: 'col-span-2',
      component: 'Textarea',
      componentProps: { rows: 2 },
    },
    {
      fieldName: 'priority',
      label: '优先级',
      component: 'InputNumber',
      defaultValue: 0,
      componentProps: { min: 0, class: '!w-full' },
    },
    {
      fieldName: 'enabled',
      label: '启用',
      component: 'Switch',
      defaultValue: true,
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'name',
      label: '规则名称',
      component: 'Input',
      componentProps: { placeholder: '请输入', clearable: true },
    },
    {
      fieldName: 'contractTypeId',
      label: '合同类型',
      component: 'ApiSelect',
      componentProps: {
        api: () =>
          getContractTypeSimpleList().then((list) =>
            (list || []).map((item) => ({ label: item.name, value: item.id })),
          ),
        allowClear: true,
        placeholder: '请选择',
      },
    },
    {
      fieldName: 'enabled',
      label: '启用',
      component: 'Select',
      componentProps: {
        options: [
          { label: '已启用', value: true },
          { label: '已禁用', value: false },
        ],
        clearable: true,
        placeholder: '请选择',
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<LegalAuditRuleApi.Rule>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'id', title: '编号', minWidth: 80 },
    { field: 'name', title: '规则名称', minWidth: 160, slots: { default: 'name' } },
    {
      field: 'ruleType',
      title: '规则类型',
      width: 120,
      formatter: ({ cellValue }) => {
        const map: Record<string, string> = {
          MANDATORY_CLAUSE: '必备条款',
          FORBIDDEN_PATTERN: '禁止表述',
          PREFERRED_CLAUSE: '推荐条款',
          CUSTOM_LLM: 'LLM补充',
        };
        return map[cellValue as string] || cellValue || 'LLM补充';
      },
    },
    { field: 'contractTypeName', title: '合同类型', minWidth: 120 },
    {
      field: 'clauseType',
      title: '条款分类',
      width: 100,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.LEGAL_CLAUSE_TYPE },
      },
    },
    { field: 'standardClauseName', title: '标准条款', minWidth: 140 },
    { field: 'priority', title: '优先级', width: 90 },
    {
      field: 'enabled',
      title: '启用',
      width: 90,
      formatter: ({ cellValue }) => (cellValue ? '是' : '否'),
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 170,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
