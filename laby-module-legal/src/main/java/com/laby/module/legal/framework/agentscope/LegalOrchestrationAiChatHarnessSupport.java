package com.laby.module.legal.framework.agentscope;

import cn.hutool.core.collection.CollUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.laby.module.ai.framework.agentscope.chat.AiChatHarnessSupport;
import com.laby.module.ai.service.chat.AiChatConversationService;
import com.laby.module.legal.enums.LegalOrchestrationConstants;
import com.laby.module.legal.framework.agentscope.middleware.LegalOrchestrationToolContextMiddleware;
import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationToolContextHolder;
import com.laby.module.legal.tool.orchestration.LegalOrchestrationToolRuntimeContext;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.tool.ToolExecutionContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 法务编排 AI 聊天 Harness 扩展
 */
@Component
public class LegalOrchestrationAiChatHarnessSupport implements AiChatHarnessSupport {

    @Resource
    private LegalOrchestrationSessionService orchestrationSessionService;
    @Resource
    private AiChatConversationService aiChatConversationService;
    @Resource
    private LegalAiPolicyResolver policyResolver;

    @Override
    public boolean supports(Long roleId, List<String> toolBeanNames) {
        if (CollUtil.isEmpty(toolBeanNames)) {
            return false;
        }
        return toolBeanNames.stream()
                .anyMatch(name -> name != null && name.startsWith(LegalOrchestrationConstants.ORCHESTRATION_TOOL_PREFIX));
    }

    @Override
    public RuntimeContext enrichRuntime(RuntimeContext.Builder builder, Long conversationId, Long userId, Long roleId) {
        Long modelId = resolveConversationModelId(conversationId);
        LegalAiPolicyBO policy = modelId != null
                ? policyResolver.resolveForConversation(conversationId, modelId)
                : null;
        LegalOrchestrationToolRuntimeContext toolContext = LegalOrchestrationToolRuntimeContext.builder()
                .conversationId(conversationId)
                .userId(userId)
                .tenantId(TenantContextHolder.getTenantId())
                .modelId(policy != null ? policy.getModelId() : modelId)
                .build();
        ToolExecutionContext toolExecutionContext = ToolExecutionContext.builder()
                .register(toolContext)
                .build();
        builder.sessionId(String.valueOf(conversationId))
                .put(LegalOrchestrationToolRuntimeContext.class, toolContext)
                .toolExecutionContext(toolExecutionContext);
        if (userId != null) {
            builder.userId(String.valueOf(userId));
        }
        LegalOrchestrationToolContextHolder.bindConversation(conversationId, toolContext);
        orchestrationSessionService.getOrCreateSession(conversationId, userId, policy);
        return builder.build();
    }

    private Long resolveConversationModelId(Long conversationId) {
        if (conversationId == null) {
            return null;
        }
        AiChatConversationDO conversation = aiChatConversationService.getChatConversation(conversationId);
        return conversation != null ? conversation.getModelId() : null;
    }

    @Override
    public List<MiddlewareBase> extraMiddlewares(Long conversationId, Long userId, Long roleId) {
        return List.of(new LegalOrchestrationToolContextMiddleware());
    }

}
