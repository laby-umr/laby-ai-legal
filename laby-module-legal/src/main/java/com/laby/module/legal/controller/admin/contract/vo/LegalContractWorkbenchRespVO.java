package com.laby.module.legal.controller.admin.contract.vo;

import com.laby.module.legal.controller.admin.opinion.vo.LegalAuditOpinionRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 合同审阅工作台 Response VO")
@Data
public class LegalContractWorkbenchRespVO {

    private LegalContractRespVO contract;

    @Schema(description = "PARAGRAPH 或 CLAUSE")
    private String navigationMode;

    private List<LegalContractWorkbenchNavigationNodeVO> navigationNodes = new ArrayList<>();

    private List<LegalContractParagraphRespVO> paragraphs = new ArrayList<>();

    private List<LegalAuditOpinionRespVO> opinions = new ArrayList<>();

    private LegalContractWorkbenchReportSummaryVO reportSummary;

}
