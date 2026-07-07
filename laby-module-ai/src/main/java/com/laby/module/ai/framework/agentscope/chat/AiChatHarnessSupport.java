package com.laby.module.ai.framework.agentscope.chat;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;

import java.util.Collections;
import java.util.List;

/**
 * AI 聊天 Harness 扩展点，供业务模块（如法务编排）注入 Tool 上下文与 Middleware。
 */
public interface AiChatHarnessSupport {

    default boolean supports(Long roleId, List<String> toolBeanNames) {
        return false;
    }

    default RuntimeContext enrichRuntime(RuntimeContext.Builder builder, Long conversationId, Long userId, Long roleId) {
        return builder.build();
    }

    default List<MiddlewareBase> extraMiddlewares(Long conversationId, Long userId, Long roleId) {
        return Collections.emptyList();
    }

}
