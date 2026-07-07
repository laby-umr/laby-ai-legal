package com.laby.module.ai.service.eval.bo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单条 RAG 测评结果
 */
@Data
@Builder
public class AiRagEvalCaseResultBO {

    private String caseId;

    private String description;

    private boolean pass;

    private boolean hitAtK;

    private double mrr;

    private double recallAtK;

    private Double topScore;

    @Builder.Default
    private List<Long> retrievedSegmentIds = new ArrayList<>();

    private String failureReason;

}
