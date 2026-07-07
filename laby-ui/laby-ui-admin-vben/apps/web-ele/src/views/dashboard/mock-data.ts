import type { AnalyticsDemoBundle, FunnelSummary, RankItem } from './types';

import dayjs from 'dayjs';

/** 看板汇总（测试数据基准） */
export const OVERVIEW_METRICS = {
  bpmTodoTotal: 28,
  bpmStartedMonth: 156,
  contractInReview: 19,
  contractArchivedMonth: 42,
  aiChatMonth: 3840,
  aiTokenMonth: 1_280_000,
  contractSubmitted: 186,
  contractAiReviewed: 142,
  contractArchivedTotal: 98,
} as const;

/** 合同类型 / 流程分类排行 */
export const MODULE_RANK_LIST: RankItem[] = [
  { nickname: '采购合同', deptName: '供应链', count: 48 },
  { nickname: '销售合同', deptName: '商务中心', count: 36 },
  { nickname: '请假审批', deptName: 'BPM·OA', count: 32 },
  { nickname: '技术服务', deptName: '研发', count: 28 },
  { nickname: '劳动合同', deptName: '人力资源', count: 24 },
  { nickname: '费用报销', deptName: 'BPM·财务', count: 22 },
  { nickname: '保密协议', deptName: '法务', count: 18 },
  { nickname: '知识库问答', deptName: 'AI', count: 15 },
];

function buildDateLabels(start: Date, end: Date, maxPoints = 14): string[] {
  const startD = dayjs(start);
  const endD = dayjs(end);
  const days = Math.min(Math.max(endD.diff(startD, 'day'), 1), 90);
  const step = Math.max(1, Math.floor(days / maxPoints));
  const labels: string[] = [];
  let cur = startD;
  while (cur.isBefore(endD) || cur.isSame(endD, 'day')) {
    labels.push(cur.format('MM-DD'));
    cur = cur.add(step, 'day');
  }
  if (labels.length === 0) {
    labels.push(endD.format('MM-DD'));
  }
  return labels.slice(0, maxPoints);
}

export function getMockFunnelSummary(): FunnelSummary {
  return {
    submitted: OVERVIEW_METRICS.contractSubmitted,
    aiReviewed: OVERVIEW_METRICS.contractAiReviewed,
    archived: OVERVIEW_METRICS.contractArchivedTotal,
  };
}

/** 看板全套 mock（BPM / AI / 合同） */
export function getMockAnalyticsBundle(
  start: Date,
  end: Date,
): AnalyticsDemoBundle {
  const labels = buildDateLabels(start, end, 14);

  const bpmTrend = labels.map((time, i) => {
    const started = 8 + Math.round(6 * Math.sin(i * 0.65) + i * 0.35);
    const finished = Math.max(3, started - Math.round(2 + Math.cos(i) * 1.5));
    return { time, startedCount: started, finishedCount: finished };
  });

  const aiTrend = labels.map((time, i) => ({
    time,
    chatCount: 120 + Math.round(80 * Math.sin(i * 0.5) + i * 12),
    tokenK: 45 + Math.round(35 * Math.cos(i * 0.4) + i * 8),
  }));

  const contractTrend = labels.map((time, i) => {
    const created = 4 + Math.round(3 * Math.sin(i * 0.55) + i * 0.25);
    return {
      time,
      createdCount: created,
      archivedCount: Math.max(1, created - (i % 3)),
    };
  });

  const chatSum = aiTrend.reduce((s, r) => s + r.chatCount, 0);
  const archivedSum = contractTrend.reduce((s, r) => s + r.archivedCount, 0);

  return {
    metrics: {
      bpmTodoCount: OVERVIEW_METRICS.bpmTodoTotal,
      contractInReview: OVERVIEW_METRICS.contractInReview,
      aiChatCount: chatSum,
      contractArchived: archivedSum,
    },
    funnel: getMockFunnelSummary(),
    bpmTrend,
    aiTrend,
    contractTrend,
    rank: [...MODULE_RANK_LIST].sort((a, b) => b.count - a.count),
  };
}
