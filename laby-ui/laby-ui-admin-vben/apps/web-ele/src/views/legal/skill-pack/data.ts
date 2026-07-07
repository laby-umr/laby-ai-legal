import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { AiModelChatRoleApi } from '#/api/ai/model/chatRole';
import type { LegalSkillPackApi } from '#/api/legal/skill-pack';

import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

import {
  getChatRolePage,
  LEGAL_CONTRACT_CHAT_ROLE_CATEGORY,
} from '#/api/ai/model/chatRole';
import {
  getLegalAgentTools,
  getSkillPackSimpleList,
} from '#/api/legal/skill-pack';

/** 加载法务合同类 AI 角色（提示词管理中的角色） */
export async function fetchLegalChatRoles() {
  const { list } = await getChatRolePage({
    pageNo: 1,
    pageSize: 100,
    category: LEGAL_CONTRACT_CHAT_ROLE_CATEGORY,
    publicStatus: true,
  });
  return list || [];
}

function filterRolesByScene(
  roles: AiModelChatRoleApi.ChatRole[],
  scene?: string,
) {
  if (!scene || scene === 'PROPOSE' || scene === 'EXPORT_SUMMARY') {
    return roles;
  }
  if (scene === 'CHAT') {
    return roles.filter((role) => {
      const name = role.name ?? '';
      return name.includes('问答') || name.includes('Agent');
    });
  }
  if (scene === 'AUDIT') {
    return roles.filter((role) => (role.name ?? '').includes('审核'));
  }
  return roles;
}

function mapChatRoleOptions(roles: AiModelChatRoleApi.ChatRole[]) {
  return roles.map((role) => ({
    label: role.description
      ? `${role.name}（${role.description}）`
      : role.name,
    value: role.id,
  }));
}

function chatRolePlaceholder(scene?: string) {
  if (scene === 'AUDIT') {
    return '选择审核提示词角色，如「法务合同审核（首轮）」';
  }
  if (scene === 'CHAT') {
    return '选择问答 Agent 角色，如「法务合同问答 Agent」';
  }
  return '请选择 AI 角色';
}

const TOOL_OPTIONS_FALLBACK = [
  { label: '检索段落', value: 'legal_search_paragraphs' },
  { label: '检索知识库', value: 'legal_search_knowledge' },
  { label: '合同元信息', value: 'legal_get_contract_meta' },
  { label: '审核意见', value: 'legal_get_audit_opinions' },
  { label: '审核报告', value: 'legal_get_audit_report' },
  { label: '对比轮次', value: 'legal_compare_audit_rounds' },
  { label: '提案采纳', value: 'legal_propose_adopt_opinion' },
  { label: '提案跳过段落', value: 'legal_propose_skip_paragraph' },
];

async function fetchLegalToolOptions() {
  try {
    const list = await getLegalAgentTools();
    if (list?.length) {
      return list.map((item) => ({
        label: item.description
          ? `${item.description} (${item.name})`
          : item.name,
        value: item.name,
      }));
    }
  } catch {
    // fallback when API unavailable
  }
  return TOOL_OPTIONS_FALLBACK;
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'id',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'code',
      label: '编码',
      rules: 'required',
      component: 'Input',
      componentProps: { placeholder: '如 legal-audit-default' },
    },
    {
      fieldName: 'name',
      label: '名称',
      rules: 'required',
      component: 'Input',
    },
    {
      fieldName: 'scene',
      label: '场景',
      rules: 'required',
      component: 'Select',
      defaultValue: 'AUDIT',
      componentProps: {
        options: getDictOptions(DICT_TYPE.LEGAL_SKILL_PACK_SCENE, 'string'),
      },
    },
    {
      fieldName: 'chatRoleId',
      label: '关联 AI 角色',
      formItemClass: 'col-span-2',
      component: 'Select',
      help: '技能包从此角色的系统提示词读取；请在「法务 → 提示词管理」维护角色内容',
      componentProps: {
        options: [],
        allowClear: true,
        filterable: true,
        placeholder: '请选择 AI 角色',
      },
      dependencies: {
        triggerFields: ['scene'],
        async componentProps(values) {
          const roles = filterRolesByScene(
            await fetchLegalChatRoles(),
            values.scene,
          );
          return {
            options: mapChatRoleOptions(roles),
            allowClear: true,
            filterable: true,
            placeholder: chatRolePlaceholder(values.scene),
          };
        },
      },
    },
    {
      fieldName: 'toolNames',
      label: 'Agent Tools',
      formItemClass: 'col-span-2',
      component: 'Select',
      help: '法务 Agent 实际可用工具，与 AI 控制台「工具管理」中 legal_* 工具同步；通用 AI 聊天不走此配置',
      componentProps: {
        options: TOOL_OPTIONS_FALLBACK,
        multiple: true,
        collapseTags: true,
        placeholder: '非法名称保存时将被过滤',
      },
      dependencies: {
        triggerFields: [''],
        async componentProps() {
          return {
            options: await fetchLegalToolOptions(),
            multiple: true,
            collapseTags: true,
            placeholder: '非法名称保存时将被过滤',
          };
        },
      },
    },
    {
      fieldName: 'modelPolicy',
      label: '模型策略 JSON',
      formItemClass: 'col-span-2',
      component: 'Textarea',
      componentProps: {
        rows: 2,
        placeholder: '{"maxTokens":8192,"preferReasoning":true}',
      },
    },
    {
      fieldName: 'description',
      label: '说明',
      formItemClass: 'col-span-2',
      component: 'Textarea',
      componentProps: { rows: 2 },
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
      label: '名称',
      component: 'Input',
      componentProps: { clearable: true },
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
      fieldName: 'enabled',
      label: '启用',
      component: 'Select',
      componentProps: {
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
        clearable: true,
      },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<LegalSkillPackApi.SkillPack>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'code', title: '编码', minWidth: 140 },
    { field: 'name', title: '名称', minWidth: 140 },
    {
      field: 'scene',
      title: '场景',
      width: 120,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.LEGAL_SKILL_PACK_SCENE },
      },
    },
    {
      field: 'chatRoleId',
      title: 'AI 角色',
      minWidth: 160,
      slots: { default: 'chatRole' },
    },
    { field: 'version', title: '版本', width: 70 },
    {
      field: 'enabled',
      title: '启用',
      width: 80,
      slots: { default: 'enabled' },
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
