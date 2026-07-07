package com.laby.module.ai.controller.admin.knowledge.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "RAG 测评报告")
@Data
public class AiRagEvalReportRespVO {

    private int totalCases;
    private int passedCases;
    private int hitAtKCases;
    private double passRate;
    private double avgMrr;
    private double hitAtKRate;
    private double avgRecallAtK;

    @Schema(description = "失败用例 ID")
    private List<String> failedCaseIds = new ArrayList<>();

    @Schema(description = "逐条结果")
    private List<CaseResult> caseResults = new ArrayList<>();

    @Data
    public static class CaseResult {
        private String caseId;
        private String description;
        private boolean pass;
        private boolean hitAtK;
        private double mrr;
        private double recallAtK;
        private Double topScore;
        private List<Long> retrievedSegmentIds = new ArrayList<>();
        private String failureReason;
    }

}
