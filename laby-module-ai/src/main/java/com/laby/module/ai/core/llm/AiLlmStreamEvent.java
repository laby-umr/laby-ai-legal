package com.laby.module.ai.core.llm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AiLlmStreamEvent {
    public enum Type { CONTENT, REASONING, DONE, ERROR }

    private Type type;
    private String delta;
    private String errorMessage;
}
