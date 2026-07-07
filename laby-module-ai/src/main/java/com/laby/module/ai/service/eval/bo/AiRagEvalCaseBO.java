package com.laby.module.ai.service.eval.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索测评用例
 */
@Data
public class AiRagEvalCaseBO {

    private String caseId;

    private String description;

    /** 在线测评：知识库编号 */
    private Long knowledgeId;

    private String query;

    private Integer topK;

    private Double similarityThreshold;

    /** 离线测评：内嵌语料（不访问 DB / Qdrant） */
    private List<AiRagEvalSegmentFixtureBO> segments = new ArrayList<>();

    private AiRagEvalExpectationBO expectation;

}
