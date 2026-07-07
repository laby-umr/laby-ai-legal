import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace LegalContractMemoryApi {
  export interface Memory {
    id?: number;
    contractId?: number;
    sessionId?: string;
    memoryType?: string;
    content?: string;
    sourceMessageId?: number;
    createTime?: string;
  }

  export interface UserFact {
    id?: number;
    userId?: number;
    contractId?: number;
    sessionId?: string;
    content?: string;
    sourceMessageId?: number;
    createTime?: string;
  }
}

/** 情节记忆分页 */
export function getContractMemoryPage(params: PageParam & Record<string, unknown>) {
  return requestClient.get<PageResult<LegalContractMemoryApi.Memory>>(
    '/legal/contract/memory/page',
    { params },
  );
}

export function createContractMemoryPage(data: {
  contractId: number;
  sessionId?: string;
  memoryType: string;
  content: string;
}) {
  return requestClient.post<number>('/legal/contract/memory/create', data);
}

export function updateContractMemoryPage(data: {
  id: number;
  contractId: number;
  memoryType: string;
  content: string;
}) {
  return requestClient.put<boolean>('/legal/contract/memory/update', data);
}

export function deleteContractMemoryPage(id: number, contractId: number) {
  return requestClient.delete('/legal/contract/memory/delete', {
    params: { id, contractId },
  });
}

/** 用户事实分页 */
export function getUserFactPage(params: PageParam & Record<string, unknown>) {
  return requestClient.get<PageResult<LegalContractMemoryApi.UserFact>>(
    '/legal/contract/user-fact/page',
    { params },
  );
}

export function createUserFact(data: {
  userId: number;
  contractId?: number;
  sessionId?: string;
  content: string;
}) {
  return requestClient.post<number>('/legal/contract/user-fact/create', data);
}

export function updateUserFact(data: {
  id: number;
  userId: number;
  contractId?: number;
  sessionId?: string;
  content: string;
}) {
  return requestClient.put<boolean>('/legal/contract/user-fact/update', data);
}

export function deleteUserFact(id: number) {
  return requestClient.delete('/legal/contract/user-fact/delete', { params: { id } });
}
