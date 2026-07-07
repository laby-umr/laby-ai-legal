package com.laby.module.legal.framework.agentscope.middleware;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.service.agent.LegalAgentStepLogContext;
import com.laby.module.legal.service.agent.LegalAgentStepLogService;
import com.laby.module.legal.service.agent.LegalContractAgentService;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Tool 执行步骤日志（替代 {@code LegalAgentToolAspect} 的 tracing 部分）。
 */
@RequiredArgsConstructor
public class LegalAgentTraceMiddleware implements MiddlewareBase {

    /** 合同问答 Agent 允许调用的只读 audit 类工具 */
    private static final Set<String> QA_ALLOWED_AUDIT_TOOLS = Set.of(
            "legal_get_audit_opinions",
            "legal_get_audit_report",
            "legal_compare_audit_rounds");

    private final LegalAgentStepLogService agentStepLogService;

    @Override
    public Flux<AgentEvent> onActing(Agent agent, ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
        String sessionId = resolveSessionId(agent);
        LegalAgentStepLogContext.bindSession(sessionId);

        String toolName = resolveToolName(input);
        Object toolInput = resolveToolInput(input);
        if (isBlockedQaReauditTool(toolName)) {
            return rejectTool(toolName, toolInput,
                    "合同问答 Agent 禁止执行审核写操作或编排类工具，请基于已有审核意见回答");
        }
        if (LegalAgentStepLogContext.isToolStepLimitReached()) {
            return rejectTool(toolName, toolInput, "已达最大工具调用次数，请基于已有信息回答");
        }
        int toolCallCount = LegalAgentStepLogContext.incrementToolCallCount();
        if (toolCallCount > LegalContractAgentService.MAX_AGENT_STEPS) {
            LegalAgentStepLogContext.markToolStepLimitReached();
            return rejectTool(toolName, toolInput,
                    "已达最大工具调用次数（" + LegalContractAgentService.MAX_AGENT_STEPS + "），请基于已有信息回答");
        }

        long start = System.currentTimeMillis();
        return next.apply(input)
                .doOnError(ex -> agentStepLogService.logError(toolName, toolInput,
                        "失败: " + StrUtil.sub(ex.getMessage(), 0, 120)))
                .doOnComplete(() -> agentStepLogService.logTool(toolName, toolInput,
                        "完成，" + (System.currentTimeMillis() - start) + "ms",
                        System.currentTimeMillis() - start))
                .doFinally(signal -> LegalAgentStepLogContext.unbindThread());
    }

    private Flux<AgentEvent> rejectTool(String toolName, Object toolInput, String message) {
        agentStepLogService.logError(toolName, toolInput, message);
        LegalAgentStepLogContext.unbindThread();
        return Flux.error(new IllegalStateException(message));
    }

    /**
     * 三链路 L3：问答 Agent 禁止非只读的 audit / orchestration 工具（防御性拦截，白名单之外一律拒绝）。
     */
    private static boolean isBlockedQaReauditTool(String toolName) {
        LegalAgentStepLogContext.State state = LegalAgentStepLogContext.getState();
        if (state == null || state.isAllowProposal()) {
            return false;
        }
        if (StrUtil.isBlank(toolName)) {
            return false;
        }
        String normalized = toolName.trim();
        if (normalized.startsWith("legal_orchestration_")) {
            return true;
        }
        return normalized.contains("audit") && !QA_ALLOWED_AUDIT_TOOLS.contains(normalized);
    }

    private static String resolveSessionId(Agent agent) {
        RuntimeContext runtimeContext = resolveRuntimeContext(agent);
        return runtimeContext != null ? runtimeContext.getSessionId() : null;
    }

    private static RuntimeContext resolveRuntimeContext(Agent agent) {
        if (agent instanceof AgentBase agentBase) {
            return agentBase.getRuntimeContext();
        }
        return null;
    }

    private static String resolveToolName(ActingInput input) {
        List<ToolUseBlock> toolCalls = input.toolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return "unknown_tool";
        }
        return StrUtil.blankToDefault(toolCalls.get(0).getName(), "unknown_tool");
    }

    private static Object resolveToolInput(ActingInput input) {
        List<ToolUseBlock> toolCalls = input.toolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> toolInput = toolCalls.get(0).getInput();
        return toolInput != null ? toolInput : Map.of();
    }

}
