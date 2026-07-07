package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 合同段落 Response VO")
@Data
public class LegalContractParagraphRespVO {

    private Long id;
    private Long contractId;
    private String paragraphId;
    private Integer sort;
    private String text;
    private String path;
    @Schema(description = "是否标记为不需 AI 审核")
    private Boolean skipAudit;

}
