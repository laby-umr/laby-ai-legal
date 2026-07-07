package com.laby.module.legal.service.orchestration;

import com.laby.module.legal.tool.orchestration.LegalOrchestrationToolRuntimeContext;

/**
 * 编排 Tool 运行时上下文存储（支持多实例 Redis）。
 */
public interface LegalOrchestrationToolContextStore {

    void bindConversation(Long conversationId, LegalOrchestrationToolRuntimeContext context);

    LegalOrchestrationToolRuntimeContext getByConversationId(Long conversationId);

    LegalOrchestrationToolRuntimeContext getBySessionId(String sessionId);

}
