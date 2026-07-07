package com.laby.module.legal.service.agent;

import cn.hutool.core.collection.CollUtil;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.harness.agent.HarnessAgent;
import com.laby.module.legal.service.agent.bo.LegalContractAgentPendingConfirmBO;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 暂存等待用户 Confirm 的 Agent 运行态（同 sessionId resume；Confirm 快照可跨实例）。
 */
@Component
public class LegalContractAgentRuntimeRegistry {

    private final ConcurrentHashMap<String, PendingRun> pendingRuns = new ConcurrentHashMap<>();

    @Resource
    private LegalContractAgentPendingConfirmStore pendingConfirmStore;
    @Resource
    private LegalAgentSessionGuard agentSessionGuard;

    public void register(String sessionId, HarnessAgent agent, RuntimeContext runtimeContext,
                         Long contractId, boolean allowProposal, String answerMode) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        PendingRun run = new PendingRun();
        run.setSessionId(sessionId);
        run.setAgent(agent);
        run.setRuntimeContext(runtimeContext);
        run.setContractId(contractId);
        run.setAllowProposal(allowProposal);
        run.setAnswerMode(answerMode);
        pendingRuns.put(sessionId, run);
    }

    public void saveConfirmEvent(String sessionId, RequireUserConfirmEvent event) {
        PendingRun run = pendingRuns.get(sessionId);
        if (run != null) {
            run.setConfirmEvent(event);
            run.setAwaitingConfirm(true);
            pendingConfirmStore.save(toSnapshot(run, event));
            agentSessionGuard.release(sessionId);
        }
    }

    public PendingRun get(String sessionId) {
        return sessionId == null ? null : pendingRuns.get(sessionId);
    }

    public boolean isAwaitingConfirm(String sessionId) {
        PendingRun run = get(sessionId);
        return run != null && run.isAwaitingConfirm();
    }

    public void remove(String sessionId) {
        if (sessionId != null) {
            pendingRuns.remove(sessionId);
            pendingConfirmStore.remove(sessionId);
        }
    }

    private static LegalContractAgentPendingConfirmBO toSnapshot(PendingRun run, RequireUserConfirmEvent event) {
        LegalContractAgentPendingConfirmBO snapshot = new LegalContractAgentPendingConfirmBO();
        snapshot.setSessionId(run.getSessionId());
        snapshot.setContractId(run.getContractId());
        snapshot.setAllowProposal(run.isAllowProposal());
        snapshot.setAnswerMode(run.getAnswerMode());
        snapshot.setConfirmId(event.getReplyId());
        snapshot.setToolCalls(toToolCallSnapshots(event.getToolCalls()));
        return snapshot;
    }

    private static List<LegalContractAgentPendingConfirmBO.ToolCallSnapshot> toToolCallSnapshots(
            List<ToolUseBlock> toolCalls) {
        List<LegalContractAgentPendingConfirmBO.ToolCallSnapshot> snapshots = new ArrayList<>();
        if (CollUtil.isEmpty(toolCalls)) {
            return snapshots;
        }
        for (ToolUseBlock toolCall : toolCalls) {
            LegalContractAgentPendingConfirmBO.ToolCallSnapshot snapshot =
                    new LegalContractAgentPendingConfirmBO.ToolCallSnapshot();
            snapshot.setId(toolCall.getId());
            snapshot.setName(toolCall.getName());
            snapshot.setInput(toolCall.getInput());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @Data
    public static class PendingRun {
        private String sessionId;
        private Long contractId;
        private boolean allowProposal;
        private String answerMode;
        private HarnessAgent agent;
        private RuntimeContext runtimeContext;
        private RequireUserConfirmEvent confirmEvent;
        private boolean awaitingConfirm;
    }

}
