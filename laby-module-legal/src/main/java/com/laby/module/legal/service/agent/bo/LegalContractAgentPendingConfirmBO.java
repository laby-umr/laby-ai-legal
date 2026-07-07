package com.laby.module.legal.service.agent.bo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Agent Confirm 待恢复快照（Redis / 跨实例 Resume）。
 */
@Data
public class LegalContractAgentPendingConfirmBO {

    private String sessionId;
    private Long contractId;
    private boolean allowProposal;
    private String answerMode;
    private String confirmId;
    private List<ToolCallSnapshot> toolCalls;

    @Data
    public static class ToolCallSnapshot {
        private String id;
        private String name;
        private Map<String, Object> input;
    }

}
