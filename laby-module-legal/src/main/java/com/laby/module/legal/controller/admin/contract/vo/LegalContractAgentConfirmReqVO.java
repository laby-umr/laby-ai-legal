package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - Agent Confirm 请求")
@Data
public class LegalContractAgentConfirmReqVO {

    @Schema(description = "会话 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @Schema(description = "Confirm 编号（RequireUserConfirmEvent.replyId）")
    private String confirmId;

    @Schema(description = "是否批准 Tool 执行", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "approved 不能为空")
    private Boolean approved;

    @Schema(description = "关联提案编号（可选，批准后可直接执行写库）")
    private String proposalNo;

}
