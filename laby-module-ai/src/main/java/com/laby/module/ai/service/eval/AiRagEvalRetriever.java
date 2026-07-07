package com.laby.module.ai.service.eval;

import com.laby.module.ai.service.eval.bo.AiRagEvalCaseBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;

import java.util.List;

/**
 * RAG 测评检索器抽象（在线 segment 服务或离线 fixture 均可实现）
 */
@FunctionalInterface
public interface AiRagEvalRetriever {

    List<AiKnowledgeSegmentSearchRespBO> search(AiRagEvalCaseBO evalCase);

}
