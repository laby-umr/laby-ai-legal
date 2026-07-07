package com.laby.module.ai.controller.admin.knowledge.vo.segment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 知识库召回诊断")
@Data
public class AiKnowledgeRecallDiagnosticsVO {

    @Schema(description = "查询意图 code")
    private String intent;

    @Schema(description = "Multi-Query 变体")
    private List<String> queryVariants = new ArrayList<>();

    @Schema(description = "Dense 命中数")
    private Integer denseHitCount;

    @Schema(description = "Sparse 命中数")
    private Integer sparseHitCount;

    @Schema(description = "融合后命中数")
    private Integer fusedHitCount;

    @Schema(description = "Rerank 后命中数")
    private Integer rerankHitCount;

    @Schema(description = "Top1 分数")
    private Double topScore;

    @Schema(description = "耗时 ms")
    private Long latencyMs;

    @Schema(description = "路径明细")
    private Map<String, Object> paths;

    @Schema(description = "备注")
    private List<String> notes = new ArrayList<>();

}
