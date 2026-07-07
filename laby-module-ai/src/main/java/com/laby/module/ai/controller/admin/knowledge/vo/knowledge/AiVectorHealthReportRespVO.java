package com.laby.module.ai.controller.admin.knowledge.vo.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 知识库向量健康检查 Response VO")
@Data
public class AiVectorHealthReportRespVO {

    private Integer knowledgeCount;
    private Integer segmentScanned;
    private Integer missingVectorId;
    private Integer missingInQdrant;
    private Integer modelMismatch;
    private Integer missingSparseText;
    private Integer repaired;
    private Integer repairFailed;
    private Integer sparseTextRepaired;
    private Boolean dryRun;
    private List<String> warnings;

}
