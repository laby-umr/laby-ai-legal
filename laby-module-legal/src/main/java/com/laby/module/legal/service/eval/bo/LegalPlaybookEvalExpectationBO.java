package com.laby.module.legal.service.eval.bo;

import lombok.Data;

/**
 * Playbook 评测用例期望
 */
@Data
public class LegalPlaybookEvalExpectationBO {

    private Integer minOpinions;

    /** 期望意见数量上限（用于「不应命中」类用例） */
    private Integer maxOpinions;

    private String titleContains;

    private String sourceType;

    private String minRiskLevel;

}
