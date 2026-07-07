package com.laby.module.legal.controller.admin.orchestration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 法务编排提案 Response VO")
@Data
public class LegalOrchestrationProposalRespVO {

    @Schema(description = "提案编号")
    private String proposalNo;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "业务参数 JSON")
    private String payloadJson;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

}
