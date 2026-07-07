import type { AiChatMessageApi } from '#/api/ai/chat/message';

/** 表格列：知识命中段数 */
export function formatKnowledgeHitCount(
  row: Pick<AiChatMessageApi.ChatMessage, 'recallDiagnostics' | 'segmentIds'>,
): string {
  const total = row.recallDiagnostics?.totalHitCount;
  if (total != null) {
    return String(total);
  }
  const segmentCount = row.segmentIds?.length ?? 0;
  return segmentCount > 0 ? String(segmentCount) : '-';
}

/** 表格列：是否触发无引用守卫 */
export function hasNoAnswerGuard(
  row: Pick<AiChatMessageApi.ChatMessage, 'recallDiagnostics'>,
): boolean {
  return row.recallDiagnostics?.noAnswerGuard === true;
}

/** 是否可查看召回诊断详情 */
export function hasRecallDiagnostics(
  row: Pick<AiChatMessageApi.ChatMessage, 'recallDiagnostics'>,
): boolean {
  const d = row.recallDiagnostics;
  if (!d) {
    return false;
  }
  return (
    d.totalHitCount != null ||
    d.noAnswerGuard != null ||
    (d.items?.length ?? 0) > 0
  );
}
