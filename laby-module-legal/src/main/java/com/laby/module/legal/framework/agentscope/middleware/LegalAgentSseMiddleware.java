package com.laby.module.legal.framework.agentscope.middleware;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.service.agent.LegalAgentStepLogContext;
import com.laby.module.legal.tool.agent.LegalAgentSseEventHolder;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.List;
import java.util.function.Function;

/**
 * Tool 执行前后写入 SSE 事件队列（tool_start / tool_end）。
 */
public class LegalAgentSseMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onActing(Agent agent, ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
        String sessionId = resolveSessionId(agent);
        LegalAgentStepLogContext.bindSession(sessionId);
        String toolName = resolveToolName(input);
        LegalAgentSseEventHolder.pushToolStart(sessionId, toolName);

        long start = System.currentTimeMillis();
        return next.apply(input)
                .doOnError(ex -> LegalAgentSseEventHolder.pushToolEnd(sessionId, toolName,
                        "失败: " + StrUtil.sub(ex.getMessage(), 0, 120)))
                .doOnComplete(() -> {
                    // 仅在没有先推送失败事件时标记完成（异常路径由 doOnError 处理）
                })
                .doFinally(signal -> {
                    if (signal == SignalType.ON_COMPLETE) {
                        LegalAgentSseEventHolder.pushToolEnd(sessionId, toolName,
                                "完成，" + (System.currentTimeMillis() - start) + "ms");
                    }
                });
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
