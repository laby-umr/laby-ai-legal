package com.laby.module.ai.framework.knowledge.retrieval;

import com.laby.module.ai.framework.knowledge.retrieval.bo.AiKnowledgeRetrievalRequest;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;

import java.util.List;

/**
 * Universal RAG 统一检索入口
 */
public interface AiKnowledgeRetrievalService {

    List<AiKnowledgeSegmentSearchRespBO> retrieve(AiKnowledgeRetrievalRequest request);

}
