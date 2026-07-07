package com.laby.module.legal.framework.agentscope.middleware;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.legal.service.agent.LegalAgentToolProvider;
import com.laby.module.legal.tool.agent.LegalAgentToolRuntimeContext;
import com.laby.module.legal.tool.agent.LegalAgentToolSupport;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * 在 Agent 调用链上恢复租户上下文（Tool 常在异步线程执行）。
 */
public class LegalTenantMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onAgent(Agent agent, AgentInput input, Function<AgentInput, Flux<AgentEvent>> next) {
        Long tenantId = resolveTenantId(agent);
        if (tenantId == null) {
            return next.apply(input);
        }
        Long previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        return next.apply(input)
                .doFinally(signal -> restoreTenant(previous));
    }

    @Override
    public Flux<AgentEvent> onActing(Agent agent, ActingInput input, Function<ActingInput, Flux<AgentEvent>> next) {
        Long tenantId = resolveTenantId(agent);
        if (tenantId == null) {
            return next.apply(input);
        }
        Long previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        return next.apply(input)
                .doFinally(signal -> restoreTenant(previous));
    }

    private static Long resolveTenantId(Agent agent) {
        RuntimeContext runtimeContext = resolveRuntimeContext(agent);
        LegalAgentToolRuntimeContext toolContext = null;
        if (runtimeContext != null) {
            toolContext = runtimeContext.get(LegalAgentToolRuntimeContext.class);
            if (toolContext == null && StrUtil.isNotBlank(runtimeContext.getSessionId())) {
                toolContext = LegalAgentToolProvider.getSessionToolContext(runtimeContext.getSessionId());
            }
        }
        Long tenantId = LegalAgentToolSupport.resolveTenantId(
                LegalAgentToolSupport.resolve(toolContext));
        if (tenantId != null) {
            return tenantId;
        }
        return TenantContextHolder.getTenantId();
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
