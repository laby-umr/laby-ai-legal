package com.laby.module.ai.framework.agentscope.chat.middleware;

import cn.hutool.core.util.StrUtil;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

/**
 * AI Chat Tool 执行轨迹日志（对齐法务 {@code LegalAgentTraceMiddleware} 思路，使用 slf4j）。
 */
@Slf4j
public class AiChatTraceMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onActing(Agent agent, ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
        String sessionId = resolveSessionId(agent);
        String toolName = resolveToolName(input);
        log.info("[ai-chat][sessionId={}] tool start: {}", sessionId, toolName);

        long start = System.currentTimeMillis();
        return next.apply(input)
                .doOnError(ex -> log.warn("[ai-chat][sessionId={}] tool {} failed: {}", sessionId, toolName,
                        StrUtil.sub(ex.getMessage(), 0, 120)))
                .doOnComplete(() -> log.info("[ai-chat][sessionId={}] tool {} done, {}ms", sessionId, toolName,
                        System.currentTimeMillis() - start));
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

}
