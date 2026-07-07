package com.laby.module.ai.controller.admin.model.vo.chatRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "AI 润色结果")
@Data
public class AiChatRolePolishRespVO {

    @Schema(description = "润色后的角色设定")
    private String systemMessage;

}
