/** 法务合同业务状态，与后端 LegalContractStatusEnum 一致 */
export const LEGAL_CONTRACT_STATUS = {
  DRAFT: 0,
  PARSING: 10,
  AI_AUDITING: 11,
  FAILED: 15,
  OPINION_REVIEW: 20,
  AI_REAUDITING: 21,
  DIRECTOR_REVIEW: 30,
  FINALIZING: 40,
  ARCHIVED: 50,
  REJECTED: 60,
  CANCELLED: 61,
} as const;

/** 与 LEGAL_CONTRACT_STATUS 相同，便于组件内简短引用 */
export const CONTRACT_STATUS = LEGAL_CONTRACT_STATUS;

/** AI 风险等级，与后端 LegalRiskLevelEnum 一致 */
export const LEGAL_RISK_LEVEL = {
  HIGH: 'HIGH',
  MEDIUM: 'MEDIUM',
  LOW: 'LOW',
} as const;

/** 二轮反馈说明最少字数，与后端 LegalContractConstants 一致 */
export const FEEDBACK_SUMMARY_MIN_LENGTH = 20;

/** 单份合同主文件大小上限（MB），与后端 LegalContractConstants 一致 */
export const MAX_CONTRACT_FILE_SIZE_MB = 30;

export const CHAT_ANSWER_MODES = [
  { label: '简短', value: 'BRIEF' as const },
  { label: '标准', value: 'STANDARD' as const },
  { label: '详细', value: 'DETAILED' as const },
];

export const TERMINAL_CONTRACT_STATUSES = new Set<number>([
  LEGAL_CONTRACT_STATUS.ARCHIVED,
  LEGAL_CONTRACT_STATUS.REJECTED,
  LEGAL_CONTRACT_STATUS.CANCELLED,
]);

/** 审核状态时间线（与业务状态顺序一致，用于详情/工作台展示） */
export const CONTRACT_STATUS_TIMELINE = [
  { status: LEGAL_CONTRACT_STATUS.PARSING, title: '解析合同' },
  { status: LEGAL_CONTRACT_STATUS.AI_AUDITING, title: 'AI 首轮审核' },
  { status: LEGAL_CONTRACT_STATUS.OPINION_REVIEW, title: '法务处置意见' },
  { status: LEGAL_CONTRACT_STATUS.AI_REAUDITING, title: 'AI 二轮审核' },
  { status: LEGAL_CONTRACT_STATUS.DIRECTOR_REVIEW, title: '法务总监确认' },
  { status: LEGAL_CONTRACT_STATUS.FINALIZING, title: '人工收尾' },
  { status: LEGAL_CONTRACT_STATUS.ARCHIVED, title: '已归档' },
] as const;

/** BPM 节点 Key → 中文名（LegalContractTaskKeyEnum + legal_contract_review.bpmn20.xml） */
export const CONTRACT_TASK_KEY_LABELS: Record<string, string> = {
  parseContract: '解析合同',
  aiRound1: 'AI 首轮审核',
  aiRound2: 'AI 二轮审核',
  aiRound2Enqueue: 'AI 二轮入队',
  awaitAiRound2: '等待 AI 二轮',
  opinionReview: '法务处置意见',
  reviewRound2: '复核二轮结果',
  secondRoundGateway: '是否二轮',
  directorGateway: '是否总监审批',
  directorReview: '法务总监确认',
  finalize: '人工收尾',
  exportReport: '导出归档',
  failed: '处理失败',
};

/** 将 BPM currentTaskKey 转为中文展示名 */
export function formatContractTaskKeyLabel(
  taskKey?: string | null,
  status?: number | null,
): string {
  if (status === LEGAL_CONTRACT_STATUS.ARCHIVED) {
    return '已归档';
  }
  if (status === LEGAL_CONTRACT_STATUS.REJECTED) {
    return '已驳回';
  }
  if (status === LEGAL_CONTRACT_STATUS.CANCELLED) {
    return '已取消';
  }
  if (status === LEGAL_CONTRACT_STATUS.FAILED) {
    return taskKey
      ? (CONTRACT_TASK_KEY_LABELS[taskKey] ?? taskKey)
      : '处理失败';
  }
  if (!taskKey) {
    return '—';
  }
  return CONTRACT_TASK_KEY_LABELS[taskKey] ?? taskKey;
}
