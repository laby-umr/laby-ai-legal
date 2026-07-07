package com.laby.module.legal.enums.agent;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent 提案状态
 */
@Getter
@AllArgsConstructor
public enum LegalAgentProposalStatusEnum {

    PENDING("PENDING"),
    EXECUTED("EXECUTED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED"),
    ;

    private final String status;

}
