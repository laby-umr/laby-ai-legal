package com.laby.module.legal.controller.admin.orchestration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 法务编排会话 Response VO")
@Data
public class LegalOrchestrationSessionRespVO {

    private Long id;

    private Long conversationId;

    private Long userId;

    private String phase;

    private Long modelId;

    private String partyRole;

    private String auditLevel;

    private Long auditRoleId;

    private String modelName;

    private Integer previewOpinionCount;

    private Integer previewHighRiskCount;

    private List<LegalOrchestrationFileItemRespVO> fileItems;

    @Schema(description = "最近一次阶段 Checkpoint 保存时间")
    private LocalDateTime checkpointSavedAt;

    @Schema(description = "Checkpoint 中记录的阶段（可与当前 phase 对比判断是否需要恢复）")
    private String checkpointPhase;

}
