package com.laby.module.legal.framework.agentscope.middleware;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.legal.service.orchestration.LegalOrchestrationToolContextHolder;
import com.laby.module.legal.tool.orchestration.LegalOrchestrationToolRuntimeContext;
import com.laby.module.legal.tool.orchestration.LegalOrchestrationToolSupport;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.tool.ToolExecutionContext;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * 编排 Tool 执行前绑定 conversation / 租户上下文。
 */
public class LegalOrchestrationToolContextMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onActing(Agent agent, ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
        RuntimeContext runtimeContext = resolveRuntimeContext(agent);
        LegalOrchestrationToolRuntimeContext toolContext = extractToolContext(runtimeContext);
        LegalOrchestrationToolRuntimeContext resolved = LegalOrchestrationToolSupport.resolve(toolContext);
        if (resolved == null || resolved.getConversationId() == null) {
            resolved = LegalOrchestrationToolSupport.merge(resolved,
                    LegalOrchestrationToolSupport.fromRuntime(runtimeContext));
        }
        if ((resolved == null || resolved.getConversationId() == null) && runtimeContext != null
                && StrUtil.isNotBlank(runtimeContext.getSessionId())) {
            resolved = LegalOrchestrationToolSupport.merge(resolved,
                    LegalOrchestrationToolContextHolder.getBySessionId(runtimeContext.getSessionId()));
        }
        resolved = LegalOrchestrationToolSupport.resolve(resolved);
        if (resolved != null) {
            LegalOrchestrationToolContextHolder.set(resolved);
        }

        Long previousTenant = TenantContextHolder.getTenantId();
        Long tenantId = LegalOrchestrationToolSupport.resolveTenantId(resolved);
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }

        return next.apply(input)
                .doFinally(signal -> {
                    LegalOrchestrationToolContextHolder.clear();
                    restoreTenant(previousTenant);
                });
    }

    private static LegalOrchestrationToolRuntimeContext extractToolContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            return null;
        }
        LegalOrchestrationToolRuntimeContext toolContext = runtimeContext.get(LegalOrchestrationToolRuntimeContext.class);
        if (toolContext != null && toolContext.getConversationId() != null) {
            return toolContext;
        }
        ToolExecutionContext toolExecutionContext = runtimeContext.getToolExecutionContext();
        if (toolExecutionContext == null) {
            toolExecutionContext = runtimeContext.asToolExecutionContext();
        }
        if (toolExecutionContext != null) {
            LegalOrchestrationToolRuntimeContext fromToolExecutionContext =
                    toolExecutionContext.get(LegalOrchestrationToolRuntimeContext.class);
            if (fromToolExecutionContext != null) {
                return LegalOrchestrationToolSupport.merge(toolContext, fromToolExecutionContext);
            }
        }
        return LegalOrchestrationToolSupport.merge(toolContext,
                LegalOrchestrationToolSupport.fromRuntime(runtimeContext));
    }

    private static RuntimeContext resolveRuntimeContext(Agent agent) {
        if (agent instanceof AgentBase agentBase) {
            return agentBase.getRuntimeContext();
        }
        return null;
    }

    private static void restoreTenant(Long previous) {
        if (previous != null) {
            TenantContextHolder.setTenantId(previous);
        } else {
            TenantContextHolder.clear();
        }
    }

}
