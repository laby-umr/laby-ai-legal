import type { AxiosRequestConfig, PageParam, PageResult } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { fetchEventSource } from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { requestClient } from '#/api/request';
import { buildAdminApiSseHeaders } from '#/api/sse-headers';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);
const accessStore = useAccessStore();

/** Axios 上传进度事件 */
export type AxiosProgressEvent = AxiosRequestConfig['onUploadProgress'];

export namespace LegalContractApi {
  export interface ContractFile {
    fileId: number;
    fileName: string;
    mainFlag?: boolean;
  }

  export interface ContractFileResp {
    id: number;
    fileId: number;
    fileName: string;
    mainFlag: boolean;
    role?: string;
    format?: string;
    url?: string;
  }

  export interface Contract {
    id: number;
    title: string;
    contractTypeId?: number;
    partyRole: string;
    auditLevel: string;
    modelId?: number;
    auditRoleId?: number;
    reauditRoleId?: number;
    editable: boolean;
    status: number;
    statusName?: string;
    bpmStatus?: number;
    processInstanceId?: string;
    currentTaskKey?: string;
    opinionEditable?: boolean;
    secondRoundApplicable?: boolean;
    opinionCompletable?: boolean;
    reviewActionVisible?: boolean;
    startAuditVisible?: boolean;
    retryVisible?: boolean;
    failReason?: string;
    hasAuditReport?: boolean;
    auditOpinionCount?: number;
    latestAuditReportRound?: number;
    auditRound?: number;
    needSecondRound?: boolean;
    feedbackSummary?: string;
    riskHighCount?: number;
    mainFileId?: number;
    sourceFormat?: string;
    parseStatus?: number;
    createSource?: string;
    createConversationId?: number;
    createTime?: string;
    files?: ContractFileResp[];
  }

  export interface CreateReq {
    title: string;
    contractTypeId?: number;
    partyRole: string;
    auditLevel: string;
    modelId?: number;
    auditRoleId?: number;
    reauditRoleId?: number;
    editable: boolean;
    files: ContractFile[];
    startUserSelectAssignees?: Record<string, number[]>;
  }

  export interface OpinionCompleteReq {
    contractId: number;
    needSecondRound: boolean;
    feedbackSummary?: string;
  }

  export interface UploadContractFileResp {
    fileId: number;
    fileName: string;
  }

  /** 0 待处置 1 采纳 2 忽略 */
  export type OpinionStatus = 0 | 1 | 2;

  export interface Opinion {
    id: number;
    contractId: number;
    auditRound?: number;
    clauseType?: string;
    riskLevel: string;
    title: string;
    content: string;
    suggestion?: string;
    paragraphId?: string;
    clauseId?: string;
    referenceClause?: string;
    sourceType?: string;
    sourceId?: string;
    sourceVersion?: string;
    fromVersionId?: number;
    changeType?: string;
    oldText?: string;
    newText?: string;
    evidenceRefs?: string;
    status: OpinionStatus;
  }

  export interface ManualOpinionReq {
    contractId: number;
    title: string;
    content: string;
    riskLevel?: string;
    clauseType?: string;
    suggestion?: string;
  }

  export interface Paragraph {
    id: number;
    contractId: number;
    paragraphId: string;
    sort: number;
    text: string;
    path?: string;
    skipAudit?: boolean;
  }

  export interface WorkbenchNavigationNode {
    id: string;
    label: string;
    level: number;
    paragraphIds: string[];
    children?: WorkbenchNavigationNode[];
  }

  export interface WorkbenchReportSummary {
    hasReport?: boolean;
    riskHighCount?: number;
    previewMarkdown?: string;
  }

  export interface WorkbenchResp {
    contract: Contract;
    navigationMode: 'PARAGRAPH' | 'CLAUSE';
    navigationNodes: WorkbenchNavigationNode[];
    paragraphs: Paragraph[];
    opinions: Opinion[];
    reportSummary?: WorkbenchReportSummary;
  }

  export interface VersionDiffItem {
    clauseId?: string;
    clauseTitle?: string;
    changeType: 'MODIFIED' | 'ADDED' | 'REMOVED' | 'UNCHANGED';
    beforeText?: string;
    afterText?: string;
    relatedOpinionIds?: number[];
  }

  export interface VersionDiffResp {
    contractId: number;
    fromVersionId: number;
    toVersionId: number;
    fromVersionNo?: number;
    toVersionNo?: number;
    diffs: VersionDiffItem[];
  }

  export type ChatAnswerMode = 'BRIEF' | 'STANDARD' | 'DETAILED';

  export interface ChatTurn {
    role: 'user' | 'assistant';
    content: string;
  }

  export interface ChatReq {
    contractId: number;
    message: string;
    answerMode?: ChatAnswerMode;
    history?: ChatTurn[];
    /** Agent 模式：启用 Tool 按需查数 */
    agentMode?: boolean;
    /** 允许 Agent 生成写操作提案（需用户 Confirm） */
    allowProposal?: boolean;
    sessionId?: string;
  }

  export interface ChatResp {
    content?: string;
    reasoningContent?: string;
    eventType?: 'tool_start' | 'tool_end' | 'proposal' | 'confirm_required' | 'error';
    toolName?: string;
    toolSummary?: string;
    confirmId?: string;
    confirmSummary?: string;
    proposalNo?: string;
    proposalAction?: 'ADOPT_OPINION' | 'SKIP_PARAGRAPH';
    proposalTitle?: string;
    proposalPayload?: string;
    sessionId?: string;
    userMessageId?: number;
    assistantMessageId?: number;
  }

  export interface ChatMessage {
    id: number;
    contractId: number;
    replyId?: number;
    type: 'user' | 'assistant' | 'summary';
    content: string;
    reasoningContent?: string;
    agentMode?: boolean;
    sessionId?: string;
    createTime?: string;
  }

  export interface ContractMemory {
    id: number;
    contractId: number;
    sessionId?: string;
    memoryType: string;
    content: string;
    sourceMessageId?: number;
    createTime?: string;
  }

  export interface AgentProposalReq {
    proposalNo: string;
  }

  export interface AgentConfirmReq {
    sessionId: string;
    confirmId?: string;
    approved: boolean;
    proposalNo?: string;
  }

  export interface AuditProgress {
    status: 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED';
    auditRound?: number;
    batchIndex?: number;
    totalBatches?: number;
    message?: string;
    reasoningContent?: string;
  }

  export interface BatchDisposeOpinionReq {
    ids: number[];
    /** 1 采纳 2 忽略 */
    status: 1 | 2;
  }
}

export const OPINION_STATUS = {
  PENDING: 0,
  ADOPTED: 1,
  IGNORED: 2,
} as const;

export function createContract(data: LegalContractApi.CreateReq) {
  return requestClient.post<number>('/legal/contract/create', data);
}

export function retryContractPipeline(contractId: number) {
  return requestClient.post<boolean>('/legal/contract/retry-pipeline', null, {
    params: { id: contractId },
  });
}

export function startContractFirstAudit(contractId: number) {
  return requestClient.post<boolean>('/legal/contract/start-first-audit', null, {
    params: { id: contractId },
  });
}

export function uploadContractFile(
  file: File,
  onUploadProgress?: AxiosProgressEvent,
) {
  return requestClient.upload<LegalContractApi.UploadContractFileResp>(
    '/legal/contract/upload',
    { file },
    { onUploadProgress },
  );
}

export function getContract(
  id: number,
  config?: Parameters<typeof requestClient.get>[1],
) {
  return requestClient.get<LegalContractApi.Contract>(
    `/legal/contract/get?id=${id}`,
    config,
  );
}

export function getContractPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<LegalContractApi.Contract>>(
    '/legal/contract/page',
    { params },
  );
}

export function completeOpinionReview(
  data: LegalContractApi.OpinionCompleteReq,
  config?: Parameters<typeof requestClient.put>[2],
) {
  return requestClient.put('/legal/contract/complete-opinion', data, config);
}

export function getOpinionList(contractId: number) {
  return requestClient.get<LegalContractApi.Opinion[]>(
    `/legal/opinion/list-by-contract?contractId=${contractId}`,
  );
}

export function adoptOpinion(id: number) {
  return requestClient.put<OpinionDocumentApplyResult>(`/legal/opinion/adopt?id=${id}`);
}

export function ignoreOpinion(id: number) {
  return requestClient.put(`/legal/opinion/ignore?id=${id}`);
}

export function revokeOpinion(id: number) {
  return requestClient.put<OpinionDocumentApplyResult>(`/legal/opinion/revoke?id=${id}`);
}

export function batchAdoptOpinions(ids: number[]) {
  return requestClient.put<OpinionDocumentApplyResult>('/legal/opinion/batch-adopt', { ids });
}

export function batchIgnoreOpinions(ids: number[]) {
  return requestClient.put('/legal/opinion/batch-ignore', { ids });
}

export function createManualOpinion(data: LegalContractApi.ManualOpinionReq) {
  return requestClient.post<number>('/legal/opinion/create-manual', {
    contractId: data.contractId,
    title: data.title,
    content: data.content,
    riskLevel: data.riskLevel || 'MEDIUM',
    clauseType: data.clauseType,
    suggestion: data.suggestion,
  });
}

export function getAuditReport(contractId: number, auditRound?: number) {
  return requestClient.get<{ content: string }>(
    '/legal/contract/audit-report',
    { params: { contractId, auditRound } },
  );
}

export function getParagraphList(contractId: number) {
  return requestClient.get<LegalContractApi.Paragraph[]>(
    `/legal/contract/list-paragraph?contractId=${contractId}`,
  );
}

export function getWorkbench(
  contractId: number,
  config?: Parameters<typeof requestClient.get>[1],
) {
  return requestClient.get<LegalContractApi.WorkbenchResp>(
    '/legal/contract/get-workbench',
    { params: { contractId }, ...config },
  );
}

export function getAuditProgress(contractId: number) {
  return requestClient.get<LegalContractApi.AuditProgress>(
    `/legal/contract/audit-progress?contractId=${contractId}`,
  );
}

export function exportReportDocx(contractId: number) {
  return requestClient.post<number>(
    `/legal/contract/export-report?contractId=${contractId}`,
  );
}

export function exportAnnotatedContractDocx(
  contractId: number,
  visibility: 'EXTERNAL' | 'INTERNAL' = 'INTERNAL',
) {
  return requestClient.post<number>('/legal/contract/export-annotated-docx', null, {
    params: { contractId, visibility },
  });
}

/** 将全部审核意见以 Word 批注写入 WORKING，供 OnlyOffice 预览 */
export function applyRiskAnnotations(contractId: number) {
  return requestClient.post<OpinionDocumentApplyResult>(
    '/legal/contract/apply-risk-annotations',
    null,
    { params: { contractId } },
  );
}

export function exportAdoptedContractDocx(
  contractId: number,
  mode: 'CLEAN' | 'TRACKED' = 'CLEAN',
  visibility: 'EXTERNAL' | 'INTERNAL' = 'INTERNAL',
) {
  return requestClient.post<number>('/legal/contract/export-adopted-docx', null, {
    params: { contractId, mode, visibility },
  });
}

export interface AdoptedExportPrecheckResp {
  adoptedCount: number;
  autoWritableCount: number;
  conflictCount: number;
  manualConfirmCount: number;
  anchorMissingCount?: number;
  anchorOrphanCount?: number;
  missingParagraphIds?: string[];
  orphanBookmarkNames?: string[];
}

export function precheckAdoptedExport(contractId: number) {
  return requestClient.post<AdoptedExportPrecheckResp>('/legal/contract/precheck-adopted-export', null, {
    params: { contractId },
  });
}

export function repairWorkingVersion(contractId: number) {
  return requestClient.post<boolean>('/legal/contract/repair-working-version', null, {
    params: { contractId },
  });
}

export function exportArchiveZip(contractId: number) {
  return requestClient.post<number>('/legal/contract/export-archive-zip', null, {
    params: { contractId },
  });
}

export function exportDeliveryBundle(contractId: number) {
  return requestClient.post<number>('/legal/contract/export-bundle', null, {
    params: { contractId },
  });
}

export function getVersionDiff(
  contractId: number,
  fromVersionId: number,
  toVersionId: number,
) {
  return requestClient.get<LegalContractApi.VersionDiffResp>(
    '/legal/contract/version-diff',
    { params: { contractId, fromVersionId, toVersionId } },
  );
}

export interface ContractVersion {
  id: number;
  contractId: number;
  auditRound: number;
  versionNo: number;
  type: string;
  sourceVersionId?: number;
  fileId: number;
  visibility: 'EXTERNAL' | 'INTERNAL';
  immutableHash?: string;
  createTime?: string;
}

export function getContractVersionList(contractId: number) {
  return requestClient.get<ContractVersion[]>('/legal/contract/version-list', {
    params: { contractId },
  });
}

/** 下载合同附件（带登录态，避免直接打开存储外链 404） */
export function downloadContractFile(fileId: number) {
  return requestClient.download(
    `/legal/contract/download-file?fileId=${fileId}`,
  );
}

/** 四件套交付物类型（DELIV-001） */
export type ContractDeliverable = 'ORIGINAL' | 'ANNOTATED' | 'REVISION' | 'ADOPTED';

export interface SyncWorkingDocumentResp {
  revision?: string;
  workingFileId?: number;
}

/** OnlyOffice forceSave 后同步 WORKING revision */
export function syncWorkingDocument(contractId: number) {
  return requestClient.post<SyncWorkingDocumentResp>(
    '/legal/contract/document/sync-working',
    null,
    { params: { contractId } },
  );
}

/** 按需下载四件套交付物（不走版本缓存） */
export function downloadDeliverable(
  contractId: number,
  deliverable: ContractDeliverable,
  auditRound?: number,
) {
  return requestClient.download('/legal/contract/download-deliverable', {
    params: { contractId, deliverable, auditRound },
  });
}

export interface DocumentPreviewConfig {
  enabled: boolean;
  documentServerUrl?: string;
  config?: Record<string, unknown>;
  documentRevision?: string;
  editable?: boolean;
}

export interface OpinionDocumentApplyResult {
  documentUpdated?: boolean;
  documentRevision?: string;
}

/** OnlyOffice 编辑/预览配置（JWT + 拉流地址） */
export function getDocumentPreviewConfig(contractId: number) {
  return requestClient.get<DocumentPreviewConfig>(
    '/legal/document/preview-config',
    { params: { contractId } },
  );
}

export function chatContract(data: LegalContractApi.ChatReq) {
  return requestClient.post<LegalContractApi.ChatResp>('/legal/contract/chat', data);
}

/** 合同问答消息列表 */
export function getContractChatMessageList(contractId: number, sessionId?: string) {
  return requestClient.get<LegalContractApi.ChatMessage[]>('/legal/contract/chat-message/list', {
    params: { contractId, sessionId },
  });
}

/** 合同情节记忆列表 */
export function getContractMemoryList(contractId: number, sessionId?: string) {
  return requestClient.get<LegalContractApi.ContractMemory[]>(
    '/legal/contract/memory/list',
    { params: { contractId, sessionId } },
  );
}

export function deleteContractMemory(id: number, contractId: number) {
  return requestClient.delete('/legal/contract/memory/delete', {
    params: { id, contractId },
  });
}

export function createContractMemory(data: {
  contractId: number;
  sessionId?: string;
  memoryType: string;
  content: string;
}) {
  return requestClient.post<number>('/legal/contract/memory/create', data);
}

export function updateContractMemory(data: {
  id: number;
  contractId: number;
  memoryType: string;
  content: string;
}) {
  return requestClient.put<boolean>('/legal/contract/memory/update', data);
}

/** 删除单条合同问答消息 */
export function deleteContractChatMessage(id: number) {
  return requestClient.delete('/legal/contract/chat-message/delete', {
    params: { id },
  });
}

/** 从指定消息起删除后续问答 */
export function deleteContractChatMessagesFrom(id: number) {
  return requestClient.delete('/legal/contract/chat-message/delete-from', {
    params: { id },
  });
}

/** 清空合同问答 */
export function clearContractChatMessages(contractId: number) {
  return requestClient.delete('/legal/contract/chat-message/clear', {
    params: { contractId },
  });
}

/** 发送合同问答 Stream（与 AI 对话 sendChatMessageStream 一致） */
export function sendContractChatStream(
  data: LegalContractApi.ChatReq,
  ctrl: AbortController,
  onMessage: (event: MessageEvent) => void,
  onError: (error: unknown) => void,
  onClose: () => void,
) {
  const token = accessStore.accessToken;
  if (!token) {
    throw new Error('NO_AUTH_TOKEN');
  }
  return fetchEventSource(`${apiURL}/legal/contract/chat-stream`, {
    method: 'post',
    headers: buildAdminApiSseHeaders(token),
    openWhenHidden: true,
    body: JSON.stringify(data),
    signal: ctrl.signal,
    onmessage: onMessage,
    onerror: onError,
    onclose: onClose,
  });
}

export function updateParagraphSkipAudit(
  contractId: number,
  paragraphId: string,
  skipAudit: boolean,
) {
  return requestClient.put('/legal/contract/paragraph/skip-audit', {
    contractId,
    paragraphId,
    skipAudit,
  });
}

export function executeAgentProposal(data: LegalContractApi.AgentProposalReq) {
  return requestClient.post<boolean>('/legal/contract/agent/proposal/execute', data);
}

export function cancelAgentProposal(data: LegalContractApi.AgentProposalReq) {
  return requestClient.post<boolean>('/legal/contract/agent/proposal/cancel', data);
}

/** Agent Permission Confirm（注入 ConfirmResult，原 chat SSE 续流） */
export function confirmAgent(data: LegalContractApi.AgentConfirmReq) {
  return requestClient.post<boolean>('/legal/contract/agent/confirm', data);
}

/** Agent Permission Confirm 续流（原 SSE 已断开时的降级） */
export function sendAgentConfirmStream(
  data: LegalContractApi.AgentConfirmReq,
  ctrl: AbortController,
  onMessage: (event: MessageEvent) => void,
  onError: (error: unknown) => void,
  onClose: () => void,
) {
  const token = accessStore.accessToken;
  if (!token) {
    throw new Error('NO_AUTH_TOKEN');
  }
  return fetchEventSource(`${apiURL}/legal/contract/agent/confirm-stream`, {
    method: 'post',
    headers: buildAdminApiSseHeaders(token),
    openWhenHidden: true,
    body: JSON.stringify(data),
    signal: ctrl.signal,
    onmessage: onMessage,
    onerror: onError,
    onclose: onClose,
  });
}
