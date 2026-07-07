package com.laby.module.ai.framework.agentscope.session;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * AI 对话 conversationId 级互斥，防止同一会话并发 SSE（编排双开/连点）。
 */
@Component
public class AiChatConversationStreamGuard {

    private static final String LOCK_PREFIX = "chat:conv:";

    @Resource
    private AgentScopeSessionLock agentScopeSessionLock;

    public boolean tryAcquire(Long conversationId) {
        if (conversationId == null) {
            return true;
        }
        return agentScopeSessionLock.tryAcquire(LOCK_PREFIX + conversationId);
    }

    public void release(Long conversationId) {
        if (conversationId != null) {
            agentScopeSessionLock.release(LOCK_PREFIX + conversationId);
        }
    }

}
