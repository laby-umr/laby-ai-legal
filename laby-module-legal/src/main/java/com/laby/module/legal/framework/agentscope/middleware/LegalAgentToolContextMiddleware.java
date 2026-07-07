package com.laby.module.legal.framework.agentscope.middleware;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.legal.service.agent.LegalAgentStepLogContext;
import com.laby.module.legal.tool.agent.LegalAgentToolRuntimeContext;
import com.laby.module.legal.tool.agent.LegalAgentToolSupport;
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
 * Tool 执行前绑定 session / 租户（Reactor 异步线程上 TenantContextHolder 常为空）。
 */
public class LegalAgentToolContextMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onActing(Agent agent, ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
        RuntimeContext runtimeContext = resolveRuntimeContext(agent);
        LegalAgentToolRuntimeContext toolContext = extractToolContext(runtimeContext);
        LegalAgentToolRuntimeContext resolved = LegalAgentToolSupport.resolve(toolContext);

        String sessionId = LegalAgentToolSupport.resolveSessionId(resolved);
        if (StrUtil.isBlank(sessionId) && runtimeContext != null) {
            sessionId = runtimeContext.getSessionId();
        }
        if (StrUtil.isNotBlank(sessionId)) {
            LegalAgentStepLogContext.bindSession(sessionId);
        }

        Long previousTenant = TenantContextHolder.getTenantId();
        Long tenantId = LegalAgentToolSupport.resolveTenantId(resolved);
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }

        return next.apply(input)
                .doFinally(signal -> restoreTenant(previousTenant));
    }

    private static LegalAgentToolRuntimeContext extractToolContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            return null;
        }
        LegalAgentToolRuntimeContext toolContext = runtimeContext.get(LegalAgentToolRuntimeContext.class);
        if (toolContext != null) {
            return toolContext;
        }
        ToolExecutionContext toolExecutionContext = runtimeContext.getToolExecutionContext();
        if (toolExecutionContext == null) {
            toolExecutionContext = runtimeContext.asToolExecutionContext();
        }
        if (toolExecutionContext != null) {
            LegalAgentToolRuntimeContext registered = toolExecutionContext.get(LegalAgentToolRuntimeContext.class);
            if (registered != null) {
                return registered;
            }
        }
        return null;
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
