package com.laby.module.legal.framework.agentscope.chat;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 法务合同 Agent Compaction 摘要落库上下文。
 */
@Data
@Accessors(chain = true)
public class LegalContractCompactionSummaryContext {

    private Long contractId;
    private Long userId;
    private String sessionId;

    public static LegalContractCompactionSummaryContext of(Long contractId, Long userId, String sessionId) {
        return new LegalContractCompactionSummaryContext()
                .setContractId(contractId)
                .setUserId(userId)
                .setSessionId(sessionId);
    }

}
