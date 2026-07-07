package com.laby.module.ai.framework.agentscope.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnExpression("'${laby.ai.agentscope.session-store:workspace}' != 'redis'")
public class InMemoryAgentScopeSessionLock implements AgentScopeSessionLock {

    private final ConcurrentHashMap<String, Boolean> runningSessions = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return true;
        }
        return runningSessions.putIfAbsent(sessionId, Boolean.TRUE) == null;
    }

    @Override
    public void release(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            runningSessions.remove(sessionId);
        }
    }

}
