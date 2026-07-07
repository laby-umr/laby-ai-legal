package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 审核报告 Response VO")
@Data
public class LegalAuditReportRespVO {

    private Long id;
    private Long contractId;
    private Integer auditRound;
    private String content;

}
