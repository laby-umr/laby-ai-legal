import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace LegalContractTypeApi {
  /** 合同类型 */
  export interface ContractType {
    id?: number;
    name: string;
    code?: string;
    description?: string;
    knowledgeId?: number;
    defaultSkillPackIdAudit?: number;
    defaultSkillPackIdChat?: number;
    status?: number;
    sort?: number;
    createTime?: string;
  }

  export interface ConfigCheckItem {
    key: string;
    label: string;
    ok?: boolean;
    hint?: string;
  }

  export interface SkillPackSummary {
    id?: number;
    name?: string;
    scene?: string;
    chatRoleId?: number;
    chatRoleName?: string;
    toolNames?: string[];
    configured?: boolean;
  }

  export interface ConfigOverview {
    contractTypeId?: number;
    contractTypeName?: string;
    knowledgeId?: number;
    knowledgeName?: string;
    enabledAuditRuleCount?: number;
    auditSkillPack?: SkillPackSummary;
    chatSkillPack?: SkillPackSummary;
    checklist?: ConfigCheckItem[];
  }

  export interface ConfigResolve {
    contractTypeId?: number;
    contractTypeName?: string;
    knowledgeId?: number;
    knowledgeName?: string;
    auditPromptSource?: string;
    auditChatRoleId?: number;
    auditChatRoleName?: string;
    auditSystemMessagePreview?: string;
    auditToolNames?: string[];
    chatSkillPack?: SkillPackSummary;
  }
}

/** 查询合同类型分页 */
export function getContractTypePage(params: PageParam) {
  return requestClient.get<PageResult<LegalContractTypeApi.ContractType>>(
    '/legal/contract-type/page',
    { params },
  );
}

/** 查询合同类型详情 */
export function getContractType(id: number) {
  return requestClient.get<LegalContractTypeApi.ContractType>(
    `/legal/contract-type/get?id=${id}`,
  );
}

/** 新增合同类型 */
export function createContractType(data: LegalContractTypeApi.ContractType) {
  return requestClient.post<number>('/legal/contract-type/create', data);
}

/** 修改合同类型 */
export function updateContractType(data: LegalContractTypeApi.ContractType) {
  return requestClient.put('/legal/contract-type/update', data);
}

/** 删除合同类型 */
export function deleteContractType(id: number) {
  return requestClient.delete(`/legal/contract-type/delete?id=${id}`);
}

/** 批量删除合同类型 */
export function deleteContractTypeList(ids: number[]) {
  return requestClient.delete(
    `/legal/contract-type/delete-list?ids=${ids.join(',')}`,
  );
}

/** 合同类型精简列表（下拉） */
export function getContractTypeSimpleList() {
  return requestClient.get<Pick<LegalContractTypeApi.ContractType, 'id' | 'name'>[]>(
    '/legal/contract-type/simple-list',
  );
}

/** 配置中枢概览 */
export function getContractTypeConfigOverview(id: number) {
  return requestClient.get<LegalContractTypeApi.ConfigOverview>(
    `/legal/contract-type/config-overview?id=${id}`,
  );
}

/** 模拟解析运行时 AI 配置 */
export function resolveContractTypeConfig(contractTypeId: number) {
  return requestClient.get<LegalContractTypeApi.ConfigResolve>(
    `/legal/contract-type/resolve-config?contractTypeId=${contractTypeId}`,
  );
}
