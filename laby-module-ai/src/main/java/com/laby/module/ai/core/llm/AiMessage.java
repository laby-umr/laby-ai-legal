package com.laby.module.ai.core.llm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AiMessage {
    private AiMessageRoleEnum role;
    private String content;
    /** tool 名称，role=TOOL 时使用 */
    private String toolName;
}
