import { requestClient } from '#/api/request';

/** 法务编排 AI 角色 id，与 ai_chat_role.id 一致 */
export const LEGAL_ORCHESTRATION_ROLE_ID = 123;

export namespace LegalOrchestrationApi {
  export interface Proposal {
    proposalNo: string;
    action: string;
    status: string;
    title: string;
    payloadJson?: string;
    expireTime?: string;
  }

  export interface FileItem {
    id: number;
    fileName: string;
    status: string;
    suggestedTypeId?: number;
    confirmedTypeId?: number;
    contractId?: number;
  }

  export interface Session {
    id: number;
    conversationId: number;
    userId: number;
    phase: string;
    modelId?: number;
    modelName?: string;
    partyRole?: string;
    auditLevel?: string;
    auditRoleId?: number;
    previewOpinionCount?: number;
    previewHighRiskCount?: number;
    fileItems?: FileItem[];
    checkpointSavedAt?: string;
    checkpointPhase?: string;
  }

  export interface Checkpoint {
    phase: string;
    savedAt?: string;
    conversationId?: number;
    modelId?: number;
    partyRole?: string;
    auditLevel?: string;
    auditRoleId?: number;
  }

  /** 创建合同提案 payload 中的策略字段 */
  export interface CreateContractsPolicyPayload {
    modelId?: number;
    partyRole?: string;
    auditLevel?: string;
    auditRoleId?: number;
    editable?: boolean;
    policyVersion?: string;
  }
}

export function listPendingOrchestrationProposals(conversationId: number) {
  return requestClient.get<LegalOrchestrationApi.Proposal[]>(
    '/legal/orchestration/proposal/list-pending',
    { params: { conversationId } },
  );
}

export function executeOrchestrationProposal(proposalNo: string) {
  return requestClient.post<boolean>('/legal/orchestration/proposal/execute', {
    proposalNo,
  });
}

export function cancelOrchestrationProposal(proposalNo: string) {
  return requestClient.post<boolean>('/legal/orchestration/proposal/cancel', {
    proposalNo,
  });
}

export function getOrchestrationSession(conversationId: number) {
  return requestClient.get<LegalOrchestrationApi.Session>(
    '/legal/orchestration/session/get',
    { params: { conversationId } },
  );
}

export function getOrchestrationCheckpoint(conversationId: number) {
  return requestClient.get<LegalOrchestrationApi.Checkpoint | null>(
    '/legal/orchestration/session/checkpoint',
    { params: { conversationId } },
  );
}

export function resumeOrchestrationFromCheckpoint(conversationId: number) {
  return requestClient.post<LegalOrchestrationApi.Checkpoint | null>(
    '/legal/orchestration/session/resume',
    null,
    { params: { conversationId } },
  );
}
