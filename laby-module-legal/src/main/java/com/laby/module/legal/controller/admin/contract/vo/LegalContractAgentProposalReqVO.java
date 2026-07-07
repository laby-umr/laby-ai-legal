package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - Agent 提案执行/取消 Request VO")
@Data
public class LegalContractAgentProposalReqVO {

    @Schema(description = "提案编号", example = "abc123")
    @NotBlank(message = "提案编号不能为空")
    private String proposalNo;

}
