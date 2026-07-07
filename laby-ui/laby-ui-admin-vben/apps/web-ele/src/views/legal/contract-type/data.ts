import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { LegalContractTypeApi } from '#/api/legal/contract-type';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { getSkillPackSimpleList } from '#/api/legal/skill-pack';

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
      label: '类型名称',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '请输入类型名称' },
    },
    {
      fieldName: 'code',
      label: '编码',
      component: 'Input',
      componentProps: { placeholder: '如 PURCHASE' },
    },
    {
      fieldName: 'defaultSkillPackIdAudit',
      label: '审核技能包',
      formItemClass: 'col-span-2',
      help: '新建合同时快照此包；审核提示词来自包内关联的聊天角色（优先于合同级提示词）',
      component: 'ApiSelect',
      componentProps: {
        api: () =>
          getSkillPackSimpleList('AUDIT').then((list) =>
            (list || []).map((item) => ({
              label: `${item.name} (${item.code})`,
              value: item.id,
            })),
          ),
        placeholder: '按合同类型自动选用',
        allowClear: true,
      },
    },
    {
      fieldName: 'defaultSkillPackIdChat',
      label: '对话技能包',
      formItemClass: 'col-span-2',
      help: '合同详情页 Agent 问答使用的工具集与提示词来源',
      component: 'ApiSelect',
      componentProps: {
        api: () =>
          getSkillPackSimpleList('CHAT').then((list) =>
            (list || []).map((item) => ({
              label: `${item.name} (${item.code})`,
              value: item.id,
            })),
          ),
        placeholder: '可选',
        allowClear: true,
      },
    },
    {
      fieldName: 'knowledgeId',
      label: '关联知识库',
      formItemClass: 'col-span-2',
      help: '法务审核与 Agent 检索用 RAG 语料；不读聊天角色上的知识库配置',
      component: 'ApiSelect',
      componentProps: {
        api: () =>
          import('#/api/ai/knowledge/knowledge').then((m) =>
            m.getSimpleKnowledgeList().then((list) =>
              (list || []).map((item) => ({ label: item.name, value: item.id })),
            ),
          ),
        placeholder: '可选，AI 审核时优先 RAG',
        allowClear: true,
      },
    },
    {
      fieldName: 'description',
      label: '说明',
      formItemClass: 'col-span-2',
      help: '关联审核规则请在「规则与条款 → 审核规则」维护；此处只绑定类型级默认 AI 配置',
      component: 'Textarea',
      componentProps: { rows: 3, placeholder: '类型说明' },
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

/** 列表搜索 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'name',
      label: '类型名称',
      component: 'Input',
      componentProps: { placeholder: '请输入', clearable: true },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
        placeholder: '请选择',
        clearable: true,
      },
    },
  ];
}

/** 列表列 */
export function useGridColumns(): VxeTableGridOptions<LegalContractTypeApi.ContractType>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'id', title: '编号', minWidth: 80 },
    { field: 'name', title: '类型名称', minWidth: 140 },
    { field: 'code', title: '编码', minWidth: 120 },
    { field: 'knowledgeId', title: '知识库 ID', minWidth: 100 },
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
