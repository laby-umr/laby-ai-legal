import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace AiKnowledgeKnowledgeApi {
  export interface Knowledge {
    id: number; // 编号
    name: string; // 知识库名称
    description: string; // 知识库描述
    embeddingModelId: number; // 嵌入模型编号，高质量模式时维护
    topK: number; // topK
    similarityThreshold: number; // 相似度阈值
  }
}

/** 查询知识库分页 */
export function getKnowledgePage(params: PageParam) {
  return requestClient.get<PageResult<AiKnowledgeKnowledgeApi.Knowledge>>(
    '/ai/knowledge/page',
    { params },
  );
}

/** 查询知识库详情 */
export function getKnowledge(id: number) {
  return requestClient.get<AiKnowledgeKnowledgeApi.Knowledge>(
    `/ai/knowledge/get?id=${id}`,
  );
}

/** 新增知识库 */
export function createKnowledge(data: AiKnowledgeKnowledgeApi.Knowledge) {
  return requestClient.post('/ai/knowledge/create', data);
}

/** 修改知识库 */
export function updateKnowledge(data: AiKnowledgeKnowledgeApi.Knowledge) {
  return requestClient.put('/ai/knowledge/update', data);
}

/** 删除知识库 */
export function deleteKnowledge(id: number) {
  return requestClient.delete(`/ai/knowledge/delete?id=${id}`);
}

/** 获取知识库简单列表 */
export function getSimpleKnowledgeList() {
  return requestClient.get<AiKnowledgeKnowledgeApi.Knowledge[]>(
    '/ai/knowledge/simple-list',
  );
}

export namespace AiKnowledgeRagEvalApi {
  export interface CaseResult {
    caseId: string;
    description: string;
    pass: boolean;
    hitAtK: boolean;
    mrr: number;
    recallAtK: number;
    topScore?: number;
    retrievedSegmentIds: number[];
    failureReason?: string;
  }

  export interface Report {
    totalCases: number;
    passedCases: number;
    hitAtKCases: number;
    passRate: number;
    avgMrr: number;
    hitAtKRate: number;
    avgRecallAtK: number;
    failedCaseIds: string[];
    caseResults: CaseResult[];
  }
}

/** RAG 检索测评（默认跑 classpath 在线黄金集） */
export function runKnowledgeRagEval(knowledgeId: number) {
  return requestClient.post<AiKnowledgeRagEvalApi.Report>(
    `/ai/knowledge/rag-eval?knowledgeId=${knowledgeId}`,
  );
}

export namespace AiKnowledgeVectorHealthApi {
  export interface Report {
    knowledgeCount?: number;
    segmentScanned?: number;
    missingVectorId?: number;
    missingInQdrant?: number;
    modelMismatch?: number;
    missingSparseText?: number;
    repaired?: number;
    repairFailed?: number;
    sparseTextRepaired?: number;
    dryRun?: boolean;
    warnings?: string[];
  }
}

/** 知识库向量健康检查 */
export function runKnowledgeVectorHealthCheck(
  knowledgeId?: number,
  dryRun = true,
  documentId?: number,
) {
  return requestClient.post<AiKnowledgeVectorHealthApi.Report>(
    '/ai/knowledge/vector-health-check',
    null,
    { params: { knowledgeId, documentId, dryRun } },
  );
}

export namespace AiKnowledgeRagEvalLiveCaseApi {
  export interface Expectation {
    expectedSegmentIds?: number[];
    expectedContentContains?: string[];
    minRecallAtK?: number;
    minTopScore?: number;
  }

  export interface LiveCase {
    caseId: string;
    description: string;
    query: string;
    topK?: number;
    similarityThreshold?: number;
    expectation?: Expectation;
  }
}

/** 在线 RAG 测评默认用例 */
export function getRagEvalLiveCases() {
  return requestClient.get<AiKnowledgeRagEvalLiveCaseApi.LiveCase[]>(
    '/ai/knowledge/rag-eval/live-cases',
  );
}
