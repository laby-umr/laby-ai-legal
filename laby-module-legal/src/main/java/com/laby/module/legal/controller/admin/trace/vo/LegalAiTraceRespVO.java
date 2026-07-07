package com.laby.module.legal.controller.admin.trace.vo;



import com.laby.framework.excel.core.annotations.DictFormat;

import com.laby.module.legal.enums.DictTypeConstants;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



import java.time.LocalDateTime;



@Schema(description = "管理后台 - AI 追踪 Response VO")

@Data

public class LegalAiTraceRespVO {



    @Schema(description = "编号")

    private Long id;



    @Schema(description = "链路 ID")

    private String traceId;



    @Schema(description = "合同编号")

    private Long contractId;



    @Schema(description = "场景")

    @DictFormat(DictTypeConstants.LEGAL_SKILL_PACK_SCENE)

    private String scene;



    @Schema(description = "审核轮次")

    private Integer auditRound;



    @Schema(description = "模型编号")

    private Long modelId;



    @Schema(description = "平台")

    private String platform;



    @Schema(description = "状态")

    @DictFormat(DictTypeConstants.LEGAL_AI_TRACE_STATUS)

    private String status;



    @Schema(description = "Playbook 意见数")

    private Integer deterministicCount;



    @Schema(description = "总意见数")

    private Integer opinionCount;



    @Schema(description = "耗时毫秒")

    private Long latencyMs;



    @Schema(description = "失败原因")

    private String errorMessage;



    @Schema(description = "创建时间")

    private LocalDateTime createTime;



}

