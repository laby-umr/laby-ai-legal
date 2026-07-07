package com.laby.module.legal.controller.admin.orchestration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 法务编排 Checkpoint Response VO")
@Data
public class LegalOrchestrationCheckpointRespVO {

    private String phase;

    private LocalDateTime savedAt;

    private Long conversationId;

    private Long modelId;

    private String partyRole;

    private String auditLevel;

    private Long auditRoleId;

}
