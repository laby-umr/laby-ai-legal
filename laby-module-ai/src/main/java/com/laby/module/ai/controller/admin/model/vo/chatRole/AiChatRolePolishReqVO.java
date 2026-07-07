package com.laby.module.ai.controller.admin.model.vo.chatRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "AI 润色聊天角色设定")
@Data
public class AiChatRolePolishReqVO {

    @Schema(description = "业务要求草稿或粗糙提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写润色需求")
    private String draft;

    @Schema(description = "场景说明，如：法务合同首轮审核、二轮复核", example = "法务合同首轮审核")
    private String scene;

    @Schema(description = "润色使用的模型编号（空则默认对话模型）")
    private Long modelId;

    @Schema(description = "现有角色设定（可选，在此基础上优化）")
    private String existingSystemMessage;

}
