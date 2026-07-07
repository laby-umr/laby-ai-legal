package com.laby.module.legal.enums.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 法务 Agent 步骤类型
 */
@Getter
@AllArgsConstructor
public enum LegalAgentStepTypeEnum {

    LLM("LLM"),
    TOOL("TOOL"),
    ERROR("ERROR"),
    PROPOSAL("PROPOSAL"),
    ;

    private final String type;

}
