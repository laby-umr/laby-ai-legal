package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同问答消息 Response VO")
@Data
public class LegalContractChatMessageRespVO {

    @Schema(description = "消息编号")
    private Long id;

    @Schema(description = "合同编号")
    private Long contractId;

    @Schema(description = "回复的消息编号")
    private Long replyId;

    @Schema(description = "user / assistant / summary")
    private String type;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "推理内容")
    private String reasoningContent;

    @Schema(description = "是否 Agent 模式")
    private Boolean agentMode;

    @Schema(description = "问答 session")
    private String sessionId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
