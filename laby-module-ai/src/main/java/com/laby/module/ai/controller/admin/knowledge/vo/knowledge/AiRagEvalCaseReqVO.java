package com.laby.module.ai.controller.admin.knowledge.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "RAG 测评 - 单条用例")
@Data
public class AiRagEvalCaseReqVO {

    @Schema(description = "用例 ID")
    private String caseId;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "检索 query")
    private String query;

    @Schema(description = "TopK")
    private Integer topK;

    @Schema(description = "相似度阈值")
    private Double similarityThreshold;

    @Schema(description = "期望")
    private Expectation expectation;

    @Data
    public static class Expectation {

        @Schema(description = "期望 segmentId 列表")
        private List<Long> expectedSegmentIds = new ArrayList<>();

        @Schema(description = "期望 TopK 内容包含关键词（任一）")
        private List<String> expectedContentContains = new ArrayList<>();

        @Schema(description = "最低 Recall@K")
        private Double minRecallAtK;

        @Schema(description = "Top1 最低分数")
        private Double minTopScore;
    }

}
