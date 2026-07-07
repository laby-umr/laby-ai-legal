import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace AiKnowledgeSegmentApi {
  export interface KnowledgeSegment {
    id: number; // 编号
    documentId: number; // 文档编号
    knowledgeId: number; // 知识库编号
    parentId?: number; // 父分段编号
    chunkLevel?: number; // 层级 0=子块 1=父块
    blockType?: string; // 块类型 code
    headingPath?: string; // 章节路径
    pageStart?: number; // 起始页码
    pageEnd?: number; // 结束页码
    sourceLocator?: string; // 定位符
    vectorId: string; // 向量库编号
    content: string; // 切片内容
    embedText?: string; // 送入 embedding 的文本
    sparseText?: string; // 全文检索 sparse 文本
    contentLength: number; // 切片内容长度
    tokens: number; // token 数量
    retrievalCount: number; // 召回次数
    status: number; // 文档状态
    createTime: number; // 创建时间
  }
}

/** 查询知识库分段分页 */
export function getKnowledgeSegmentPage(params: PageParam) {
  return requestClient.get<PageResult<AiKnowledgeSegmentApi.KnowledgeSegment>>(
    '/ai/knowledge/segment/page',
    { params },
  );
}

/** 查询知识库分段详情 */
export function getKnowledgeSegment(id: number) {
  return requestClient.get<AiKnowledgeSegmentApi.KnowledgeSegment>(
    `/ai/knowledge/segment/get?id=${id}`,
  );
}

/** 新增知识库分段 */
export function createKnowledgeSegment(
  data: AiKnowledgeSegmentApi.KnowledgeSegment,
) {
  return requestClient.post('/ai/knowledge/segment/create', data);
}

/** 修改知识库分段 */
export function updateKnowledgeSegment(
  data: AiKnowledgeSegmentApi.KnowledgeSegment,
) {
  return requestClient.put('/ai/knowledge/segment/update', data);
}

/** 修改知识库分段状态 */
export function updateKnowledgeSegmentStatus(id: number, status: number) {
  return requestClient.put('/ai/knowledge/segment/update-status', {
    id,
    status,
  });
}

/** 删除知识库分段 */
export function deleteKnowledgeSegment(id: number) {
  return requestClient.delete(`/ai/knowledge/segment/delete?id=${id}`);
}

/** 切片内容（下载文件 + 解析 + 分段，大文件耗时较长） */
export function splitContent(url: string, segmentMaxTokens: number) {
  return requestClient.get('/ai/knowledge/segment/split', {
    params: { url, segmentMaxTokens },
    timeout: 120_000,
  });
}

/** 文档向量化进度 */
export interface KnowledgeSegmentProcess {
  documentId: number;
  count: number;
  embeddingCount: number;
  /** 入库状态：0待处理 10分段中 20向量化中 30成功 40失败 */
  ingestStatus?: number;
  ingestError?: string;
}

/** 获取文档处理列表 */
export function getKnowledgeSegmentProcessList(documentIds: number[]) {
  return requestClient.get<KnowledgeSegmentProcess[]>(
    '/ai/knowledge/segment/get-process-list',
    {
      params: { documentIds: documentIds.join(',') },
    },
  );
}

/** 召回诊断 */
export interface RecallDiagnostics {
  intent?: string;
  queryVariants?: string[];
  denseHitCount?: number;
  sparseHitCount?: number;
  fusedHitCount?: number;
  rerankHitCount?: number;
  topScore?: number;
  latencyMs?: number;
  notes?: string[];
}

/** 搜索知识库分段（含诊断） */
export interface KnowledgeSegmentSearchResult {
  segments: (AiKnowledgeSegmentApi.KnowledgeSegment & {
    score?: number;
    documentName?: string;
    expanded?: boolean;
  })[];
  diagnostics?: RecallDiagnostics;
}

/** 搜索知识库分段 */
export function searchKnowledgeSegment(params: any) {
  return requestClient.get<KnowledgeSegmentSearchResult>(
    '/ai/knowledge/segment/search',
    {
      params,
    },
  );
}
