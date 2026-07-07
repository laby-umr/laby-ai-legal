package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 段落不需审核标记 Request VO")
@Data
public class LegalContractParagraphSkipReqVO {

    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @NotBlank(message = "段落编号不能为空")
    private String paragraphId;

    @NotNull(message = "skipAudit 不能为空")
    private Boolean skipAudit;

}
