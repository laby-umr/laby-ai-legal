/** 与后端 AiKnowledgeDocumentIngestStatusEnum 一致 */
export const IngestStatus = {
  PENDING: 0,
  SPLITTING: 10,
  EMBEDDING: 20,
  SUCCESS: 30,
  FAILED: 40,
} as const;

const INGEST_STATUS_LABEL: Record<number, string> = {
  [IngestStatus.PENDING]: '待处理',
  [IngestStatus.SPLITTING]: '分段中',
  [IngestStatus.EMBEDDING]: '向量化中',
  [IngestStatus.SUCCESS]: '已完成',
  [IngestStatus.FAILED]: '失败',
};

export function getIngestStatusLabel(status?: number): string {
  if (status == null) {
    return INGEST_STATUS_LABEL[IngestStatus.PENDING]!;
  }
  return INGEST_STATUS_LABEL[status] ?? '未知';
}

export function isIngestRunning(status?: number): boolean {
  return status === IngestStatus.SPLITTING || status === IngestStatus.EMBEDDING;
}

export function canRetryIngest(status?: number, stale?: boolean): boolean {
  if (isIngestRunning(status)) {
    return false;
  }
  // 已完成也可重新入库（例如切换 MinerU 后重跑结构化分片）
  if (
    status === IngestStatus.SUCCESS
    || status === IngestStatus.FAILED
    || status === IngestStatus.PENDING
  ) {
    return true;
  }
  if (stale) {
    return true;
  }
  return false;
}
