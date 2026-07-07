package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 合同问答 Response VO")
@Data
public class LegalContractChatRespVO {

    @Schema(description = "回答正文（Markdown）")
    private String content;

    @Schema(description = "推理过程（若模型支持）")
    private String reasoningContent;

    @Schema(description = "事件类型：tool_start / tool_end / proposal / confirm_required / error")
    private String eventType;

    @Schema(description = "Permission Confirm 编号（replyId）")
    private String confirmId;

    @Schema(description = "Confirm 摘要")
    private String confirmSummary;

    @Schema(description = "Tool 名称")
    private String toolName;

    @Schema(description = "Tool 执行摘要")
    private String toolSummary;

    @Schema(description = "提案编号")
    private String proposalNo;

    @Schema(description = "提案动作：ADOPT_OPINION / SKIP_PARAGRAPH")
    private String proposalAction;

    @Schema(description = "提案展示标题")
    private String proposalTitle;

    @Schema(description = "提案业务参数 JSON")
    private String proposalPayload;

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "本轮 user 消息编号")
    private Long userMessageId;

    @Schema(description = "本轮 assistant 消息编号")
    private Long assistantMessageId;

}
