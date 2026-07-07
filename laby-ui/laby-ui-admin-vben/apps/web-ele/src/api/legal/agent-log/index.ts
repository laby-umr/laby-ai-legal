import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace LegalAgentStepLogApi {
  /** Agent 步骤日志 */
  export interface StepLog {
    id?: number;
    contractId?: number;
    userId?: number;
    sessionId?: string;
    stepIndex?: number;
    stepType?: string;
    toolName?: string;
    toolInputJson?: string;
    toolOutputSummary?: string;
    latencyMs?: number;
    createTime?: string;
  }
}

/** Agent 步骤日志分页 */
export function getAgentStepLogPage(params: PageParam) {
  return requestClient.get<PageResult<LegalAgentStepLogApi.StepLog>>(
    '/legal/agent-step-log/page',
    { params },
  );
}

/** 按 sessionId 查询步骤链 */
export function getAgentStepLogListBySession(sessionId: string) {
  return requestClient.get<LegalAgentStepLogApi.StepLog[]>(
    '/legal/agent-step-log/list-by-session',
    { params: { sessionId } },
  );
}
