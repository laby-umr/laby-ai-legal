package com.laby.module.legal.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - Agent 步骤日志 Response VO")
@Data
public class LegalAgentStepLogRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "合同编号")
    private Long contractId;

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "会话编号")
    private String sessionId;

    @Schema(description = "步骤序号")
    private Integer stepIndex;

    @Schema(description = "步骤类型")
    private String stepType;

    @Schema(description = "Tool 名称")
    private String toolName;

    @Schema(description = "Tool 入参 JSON")
    private String toolInputJson;

    @Schema(description = "出参摘要")
    private String toolOutputSummary;

    @Schema(description = "耗时毫秒")
    private Integer latencyMs;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
