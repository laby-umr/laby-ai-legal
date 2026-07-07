package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "审阅工作台 - 报告摘要")
@Data
public class LegalContractWorkbenchReportSummaryVO {

    private Boolean hasReport;

    private Integer riskHighCount;

    @Schema(description = "报告 Markdown 前 2000 字预览")
    private String previewMarkdown;

}
