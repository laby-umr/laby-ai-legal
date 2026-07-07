package com.laby.module.legal.controller.admin.orchestration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 法务编排文件项 Response VO")
@Data
public class LegalOrchestrationFileItemRespVO {

    private Long id;

    private String fileName;

    private String status;

    private Long suggestedTypeId;

    private Long confirmedTypeId;

    private Long contractId;

}
