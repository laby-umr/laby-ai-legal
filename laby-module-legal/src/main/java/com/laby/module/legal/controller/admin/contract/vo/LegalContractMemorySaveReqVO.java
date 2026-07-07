package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 合同情节记忆新增/修改 Request VO")
@Data
public class LegalContractMemorySaveReqVO {

    @Schema(description = "记忆编号，修改时必填")
    private Long id;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @Schema(description = "会话编号，可选")
    private String sessionId;

    @Schema(description = "记忆类型：milestone / risk / decision / fact", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "记忆类型不能为空")
    private String memoryType;

    @Schema(description = "记忆内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "记忆内容不能为空")
    private String content;

}
