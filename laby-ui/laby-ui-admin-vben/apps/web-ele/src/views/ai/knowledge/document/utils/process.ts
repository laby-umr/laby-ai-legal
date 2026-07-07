import { IngestStatus } from '#/views/ai/knowledge/document/utils/ingest';

/** 向量化进度：与后端 get-process-list 一致 */
export interface KnowledgeDocumentProcessInfo {
  documentId: number;
  count: number;
  embeddingCount: number;
  ingestStatus?: number;
  ingestError?: string;
}

export interface KnowledgeDocumentProcessState {
  progress: number;
  count: number;
  embeddingCount: number;
  ingestStatus?: number;
  ingestError?: string;
  /** 进度长时间未变化，可能任务中断 */
  stale?: boolean;
  /** 内部：上次进度变化时间戳 */
  lastChangeAt?: number;
}

/** 进度停滞超过该时间则提示「可能已中断」（毫秒） */
export const EMBEDDING_PROGRESS_STALE_MS = 5 * 60 * 1000;

export const EMBEDDING_POLL_INTERVAL_MS = 3000;

export function calcEmbeddingProgress(
  embeddingCount?: number,
  count?: number,
): number {
  if (!count || count <= 0) {
    return 0;
  }
  return Math.min(
    100,
    Math.floor(((embeddingCount || 0) / count) * 100),
  );
}

export function isEmbeddingComplete(
  progress: number,
  count?: number,
): boolean {
  return !!count && count > 0 && progress >= 100;
}

export function buildProcessState(
  info: KnowledgeDocumentProcessInfo | undefined,
  prev?: KnowledgeDocumentProcessState,
): KnowledgeDocumentProcessState {
  const count = info?.count || 0;
  const embeddingCount = info?.embeddingCount || 0;
  const progress = calcEmbeddingProgress(embeddingCount, count);
  const now = Date.now();
  let stale = false;
  if (prev && !isEmbeddingComplete(progress, count) && count > 0) {
    if (progress > (prev.progress || 0)) {
      stale = false;
    } else if (
      prev.lastChangeAt &&
      now - prev.lastChangeAt >= EMBEDDING_PROGRESS_STALE_MS
    ) {
      stale = true;
    } else {
      stale = !!prev.stale;
    }
  }
  const lastChangeAt =
    !prev || progress > (prev.progress || 0) ? now : prev.lastChangeAt || now;
  return {
    progress,
    count,
    embeddingCount,
    ingestStatus: info?.ingestStatus,
    ingestError: info?.ingestError,
    stale,
    lastChangeAt,
  };
}

export function mergeProcessList(
  infos: KnowledgeDocumentProcessInfo[],
  prevMap: Record<number, KnowledgeDocumentProcessState>,
): Record<number, KnowledgeDocumentProcessState> {
  const next = { ...prevMap };
  for (const info of infos) {
    next[info.documentId] = buildProcessState(info, prevMap[info.documentId]);
  }
  return next;
}

export function getProcessStatusLabel(
  state: KnowledgeDocumentProcessState | undefined,
): string {
  if (state?.ingestStatus === IngestStatus.FAILED) {
    return '入库失败';
  }
  if (state?.ingestStatus === IngestStatus.SPLITTING) {
    return '分段中';
  }
  if (state?.ingestStatus === IngestStatus.PENDING && !state.count) {
    return '等待向量化';
  }
  if (!state || !state.count) {
    return '等待向量化';
  }
  if (
    isEmbeddingComplete(state.progress, state.count) ||
    state.ingestStatus === IngestStatus.SUCCESS
  ) {
    return '已完成';
  }
  if (state.stale && state.ingestStatus !== IngestStatus.FAILED) {
    return '可能已中断';
  }
  if (state.ingestStatus === IngestStatus.EMBEDDING) {
    return '向量化中';
  }
  return '向量化中';
}

export function shouldPollProcess(
  state: KnowledgeDocumentProcessState | undefined,
): boolean {
  if (!state) {
    return true;
  }
  if (state.ingestStatus === IngestStatus.FAILED) {
    return false;
  }
  if (state.ingestStatus === IngestStatus.SUCCESS && isEmbeddingComplete(state.progress, state.count)) {
    return false;
  }
  if (isEmbeddingComplete(state.progress, state.count) && state.ingestStatus !== IngestStatus.EMBEDDING) {
    return false;
  }
  return true;
}
