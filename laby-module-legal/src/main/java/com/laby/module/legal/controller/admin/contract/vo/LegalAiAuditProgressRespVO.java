package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 审核进度 Response VO")
@Data
public class LegalAiAuditProgressRespVO {

    @Schema(description = "RUNNING / COMPLETED / FAILED / IDLE")
    private String status;

    @Schema(description = "审核轮次")
    private Integer auditRound;

    @Schema(description = "当前批次（从 1 开始）")
    private Integer batchIndex;

    @Schema(description = "总批次数")
    private Integer totalBatches;

    @Schema(description = "进度说明")
    private String message;

    @Schema(description = "模型推理过程（流式累积，仅展示用）")
    private String reasoningContent;

}
