package com.laby.module.legal.service.orchestration;

import com.laby.module.legal.tool.orchestration.LegalOrchestrationToolRuntimeContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM 内存编排 Tool 上下文（单实例 / Redis 降级）。
 */
public class InMemoryLegalOrchestrationToolContextStore implements LegalOrchestrationToolContextStore {

    private final ConcurrentHashMap<Long, LegalOrchestrationToolRuntimeContext> conversationCache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LegalOrchestrationToolRuntimeContext> sessionCache =
            new ConcurrentHashMap<>();

    @Override
    public void bindConversation(Long conversationId, LegalOrchestrationToolRuntimeContext context) {
        if (conversationId == null || context == null) {
            return;
        }
        conversationCache.put(conversationId, context);
        sessionCache.put(String.valueOf(conversationId), context);
    }

    @Override
    public LegalOrchestrationToolRuntimeContext getByConversationId(Long conversationId) {
        if (conversationId == null) {
            return null;
        }
        return conversationCache.get(conversationId);
    }

    @Override
    public LegalOrchestrationToolRuntimeContext getBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessionCache.get(sessionId);
    }

}
