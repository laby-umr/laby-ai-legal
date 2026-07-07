package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 完成意见处置 Request VO")
@Data
public class LegalContractOpinionCompleteReqVO {

    @Schema(description = "合同编号", required = true)
    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @Schema(description = "是否申请二轮 AI", required = true)
    @NotNull(message = "是否二轮不能为空")
    private Boolean needSecondRound;

    @Schema(description = "二轮反馈说明，申请二轮时必填")
    private String feedbackSummary;

}
