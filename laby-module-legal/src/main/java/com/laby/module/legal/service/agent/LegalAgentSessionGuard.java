package com.laby.module.legal.service.agent;

import com.laby.module.ai.framework.agentscope.session.AgentScopeSessionLock;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 合同 Agent 会话级互斥，避免同一 sessionId 并发 streamEvents 触发 AgentScope「Agent is still running」。
 */
@Component
public class LegalAgentSessionGuard {

    @Resource
    private AgentScopeSessionLock agentScopeSessionLock;

    public boolean tryAcquire(String sessionId) {
        return agentScopeSessionLock.tryAcquire(sessionId);
    }

    public void release(String sessionId) {
        agentScopeSessionLock.release(sessionId);
    }

}
