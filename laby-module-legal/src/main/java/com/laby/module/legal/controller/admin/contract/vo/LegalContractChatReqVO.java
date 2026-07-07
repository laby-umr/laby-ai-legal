package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 合同问答 Request VO")
@Data
public class LegalContractChatReqVO {

    @Schema(description = "合同编号")
    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @Schema(description = "用户问题")
    @NotBlank(message = "问题不能为空")
    private String message;

    @Schema(description = "回答模式：BRIEF / STANDARD / DETAILED", example = "STANDARD")
    private String answerMode;

    @Schema(description = "多轮上下文（不含本轮问题）")
    private List<ChatTurn> history;

    @Schema(description = "是否启用 Agent 模式（Tool 按需查数）")
    private Boolean agentMode;

    @Schema(description = "是否允许 Agent 生成写操作提案（需用户 Confirm）")
    private Boolean allowProposal;

    @Schema(description = "前端会话 ID，用于步骤日志关联（Phase 2）")
    private String sessionId;

    @Data
    public static class ChatTurn {
        @Schema(description = "user / assistant")
        private String role;
        private String content;
    }

}
