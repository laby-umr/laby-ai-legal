/** 看板 mock 数据类型（BPM / AI / 法务合同） */

/** 合同审核漏斗：提交 → AI 审核 → 归档 */
export interface FunnelSummary {
  /** 提交审核 */
  submitted: number;
  /** AI 审核完成（含首轮/二轮） */
  aiReviewed: number;
  /** 已归档 */
  archived: number;
}

/** BPM 流程趋势（按日） */
export interface BpmTrendByDate {
  /** 流程发起数 */
  startedCount: number;
  /** 流程办结数 */
  finishedCount: number;
  time: string;
}

/** AI 使用趋势（按日） */
export interface AiTrendByDate {
  /** 对话次数 */
  chatCount: number;
  /** Token 消耗（千） */
  tokenK: number;
  time: string;
}

/** 合同业务趋势（按日/月） */
export interface ContractTrendByDate {
  /** 新增合同 */
  createdCount: number;
  /** 完成归档 */
  archivedCount: number;
  time: string;
}

export interface RankItem {
  count: number;
  deptName: string;
  nickname: string;
}

export interface DashboardMetrics {
  /** BPM 待办任务 */
  bpmTodoCount: number;
  /** 合同审核中 */
  contractInReview: number;
  /** AI 对话次数 */
  aiChatCount: number;
  /** 合同本期归档 */
  contractArchived: number;
}

export interface AnalyticsDemoBundle {
  aiTrend: AiTrendByDate[];
  bpmTrend: BpmTrendByDate[];
  contractTrend: ContractTrendByDate[];
  funnel: FunnelSummary;
  metrics: DashboardMetrics;
  rank: RankItem[];
}

/** 周趋势折线（chart-options 工作台辅助，保留类型） */
export interface CustomerSummaryByDate {
  customerCreateCount: number;
  customerDealCount: number;
  time: string;
}
