import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace LegalStandardClauseApi {
  /** 标准条款 */
  export interface Clause {
    id?: number;
    name: string;
    clauseType?: string;
    categoryScope?: string;
    content: string;
    referenceSource?: string;
    status?: number;
    sort?: number;
    createTime?: string;
  }
}

/** 查询标准条款分页 */
export function getStandardClausePage(params: PageParam) {
  return requestClient.get<PageResult<LegalStandardClauseApi.Clause>>(
    '/legal/standard-clause/page',
    { params },
  );
}

/** 查询标准条款详情 */
export function getStandardClause(id: number) {
  return requestClient.get<LegalStandardClauseApi.Clause>(
    `/legal/standard-clause/get?id=${id}`,
  );
}

/** 新增标准条款 */
export function createStandardClause(data: LegalStandardClauseApi.Clause) {
  return requestClient.post<number>('/legal/standard-clause/create', data);
}

/** 修改标准条款 */
export function updateStandardClause(data: LegalStandardClauseApi.Clause) {
  return requestClient.put('/legal/standard-clause/update', data);
}

/** 删除标准条款 */
export function deleteStandardClause(id: number) {
  return requestClient.delete(`/legal/standard-clause/delete?id=${id}`);
}

/** 批量删除标准条款 */
export function deleteStandardClauseList(ids: number[]) {
  return requestClient.delete(
    `/legal/standard-clause/delete-list?ids=${ids.join(',')}`,
  );
}

/** 标准条款精简列表 */
export function getStandardClauseSimpleList() {
  return requestClient.get<
    Pick<LegalStandardClauseApi.Clause, 'id' | 'name' | 'clauseType'>[]
  >('/legal/standard-clause/simple-list');
}
