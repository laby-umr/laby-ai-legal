package com.laby.module.ai.service.eval.bo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索测评报告
 */
@Data
@Builder
public class AiRagEvalReportBO {

    private boolean dryRun;

    private int totalCases;

    private int passedCases;

    private int hitAtKCases;

    @Builder.Default
    private List<String> failedCaseIds = new ArrayList<>();

    @Builder.Default
    private List<AiRagEvalCaseResultBO> caseResults = new ArrayList<>();

    /** 平均 MRR */
    private double avgMrr;

    /** Hit@K 比例 */
    private double hitAtKRate;

    /** 平均 Recall@K */
    private double avgRecallAtK;

    public double passRate() {
        return totalCases == 0 ? 0D : (double) passedCases / totalCases;
    }

    public boolean meetsPassRateThreshold(double minPassRate) {
        if (minPassRate <= 0D) {
            return true;
        }
        return passRate() + 1e-9 >= minPassRate;
    }

    public boolean meetsHitAtKThreshold(double minHitAtKRate) {
        if (minHitAtKRate <= 0D) {
            return true;
        }
        return getHitAtKRate() + 1e-9 >= minHitAtKRate;
    }

}
