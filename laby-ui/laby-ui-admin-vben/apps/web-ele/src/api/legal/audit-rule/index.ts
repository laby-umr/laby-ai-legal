import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace LegalAuditRuleApi {
  /** 审核规则 */
  export interface Rule {
    id?: number;
    name: string;
    contractTypeId?: number;
    contractTypeName?: string;
    clauseType?: string;
    standardClauseId?: number;
    standardClauseName?: string;
    ruleContent?: string;
    priority?: number;
    enabled?: boolean;
    description?: string;
    ruleType?: string;
    matchPattern?: string;
    matchType?: string;
    riskLevel?: string;
    actionOnHit?: string;
    playbookVersion?: number;
    createTime?: string;
  }
}

/** 查询审核规则分页 */
export function getAuditRulePage(params: PageParam) {
  return requestClient.get<PageResult<LegalAuditRuleApi.Rule>>(
    '/legal/audit-rule/page',
    { params },
  );
}

/** 查询审核规则详情 */
export function getAuditRule(id: number) {
  return requestClient.get<LegalAuditRuleApi.Rule>(
    `/legal/audit-rule/get?id=${id}`,
  );
}

/** 新增审核规则 */
export function createAuditRule(data: LegalAuditRuleApi.Rule) {
  return requestClient.post<number>('/legal/audit-rule/create', data);
}

/** 修改审核规则 */
export function updateAuditRule(data: LegalAuditRuleApi.Rule) {
  return requestClient.put('/legal/audit-rule/update', data);
}

/** 修改审核规则启用状态 */
export function updateAuditRuleEnabled(id: number, enabled: boolean) {
  return requestClient.put(
    `/legal/audit-rule/update-enabled?id=${id}&enabled=${enabled}`,
  );
}

/** 删除审核规则 */
export function deleteAuditRule(id: number) {
  return requestClient.delete(`/legal/audit-rule/delete?id=${id}`);
}

/** 批量删除审核规则 */
export function deleteAuditRuleList(ids: number[]) {
  return requestClient.delete(
    `/legal/audit-rule/delete-list?ids=${ids.join(',')}`,
  );
}
