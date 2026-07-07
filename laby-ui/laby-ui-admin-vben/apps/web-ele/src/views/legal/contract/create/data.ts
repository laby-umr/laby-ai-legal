import type { VbenFormSchema } from '#/adapter/form';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import { z } from '#/adapter/form';

/** 新建合同审核表单（双列布局） */
export function useCreateFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'title',
      label: '合同标题',
      component: 'Input',
      formItemClass: 'col-span-2',
      componentProps: {
        placeholder: '请输入合同标题',
        maxlength: 200,
      },
      rules: z.string().min(1, '请填写合同标题'),
    },
    {
      fieldName: 'contractTypeId',
      label: '合同类型',
      component: 'Select',
      formItemClass: 'col-span-1',
      help: '决定默认技能包快照与 RAG 知识库；检查逻辑请在「审核规则」维护',
      componentProps: {
        placeholder: '推荐选择，关联知识库与规则',
        filterable: true,
        clearable: true,
        options: [],
      },
    },
    {
      fieldName: 'partyRole',
      label: '我方立场',
      component: 'Select',
      formItemClass: 'col-span-1',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_PARTY_ROLE, 'string'),
        placeholder: '请选择',
      },
      defaultValue: 'A',
      rules: 'required',
    },
    {
      fieldName: 'auditLevel',
      label: '审核强度',
      component: 'Select',
      formItemClass: 'col-span-1',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_AUDIT_LEVEL, 'string'),
        placeholder: '请选择',
      },
      defaultValue: 'standard',
      rules: 'required',
    },
    {
      fieldName: 'auditRoleId',
      label: '审核提示词（高级覆盖）',
      help: '默认走：合同类型 → 技能包 → 聊天角色；需 advanced 权限可见',
      component: 'Select',
      formItemClass: 'col-span-1',
      componentProps: {
        placeholder: '首轮角色（可选）',
        filterable: true,
        options: [],
      },
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'reauditRoleId',
      label: '二轮提示词（高级覆盖）',
      help: '留空则沿用首轮或技能包配置',
      component: 'Select',
      formItemClass: 'col-span-1',
      componentProps: {
        placeholder: '可选',
        filterable: true,
        clearable: true,
        options: [],
      },
      dependencies: {
        triggerFields: ['reauditRoleId'],
        show: () => false,
      },
    },
    {
      fieldName: 'modelId',
      label: 'AI 模型',
      component: 'Select',
      formItemClass: 'col-span-1',
      componentProps: {
        placeholder: '请选择模型',
        filterable: true,
        options: [],
      },
      rules: z.number({ required_error: '请选择 AI 审核模型' }),
    },
    {
      fieldName: 'editable',
      label: '可编辑',
      component: 'Switch',
      formItemClass: 'col-span-1',
      defaultValue: true,
    },
    {
      fieldName: 'contractFile',
      label: '合同文件',
      component: 'Upload',
      formItemClass: 'col-span-2',
      rules: 'required',
    },
  ];
}
