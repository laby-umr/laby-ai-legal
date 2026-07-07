import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace LegalAiTraceApi {
  export interface Trace {
    id?: number;
    traceId?: string;
    contractId?: number;
    scene?: string;
    auditRound?: number;
    modelId?: number;
    platform?: string;
    status?: string;
    deterministicCount?: number;
    opinionCount?: number;
    latencyMs?: number;
    errorMessage?: string;
    createTime?: string;
  }
}

export function getAiTracePage(params: PageParam & {
  contractId?: number;
  scene?: string;
  status?: string;
}) {
  return requestClient.get<PageResult<LegalAiTraceApi.Trace>>(
    '/legal/ai-trace/page',
    { params },
  );
}
