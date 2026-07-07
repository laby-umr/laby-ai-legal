package com.laby.module.legal.tool.orchestration;



import cn.hutool.core.util.StrUtil;

import com.laby.framework.security.core.util.SecurityFrameworkUtils;

import com.laby.framework.tenant.core.context.TenantContextHolder;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationToolContextHolder;

import io.agentscope.core.agent.RuntimeContext;



import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;

import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_SESSION_NOT_EXISTS;



/**

 * 编排 Tool 公共上下文读取

 */

public final class LegalOrchestrationToolSupport {



    private LegalOrchestrationToolSupport() {

    }



    public static LegalOrchestrationToolRuntimeContext resolve(LegalOrchestrationToolRuntimeContext toolContext) {

        if (toolContext != null && toolContext.getConversationId() != null && toolContext.getUserId() != null) {

            return toolContext;

        }



        LegalOrchestrationToolRuntimeContext resolved = merge(toolContext, LegalOrchestrationToolContextHolder.get());



        Long conversationId = resolved != null ? resolved.getConversationId() : null;

        if (conversationId == null && toolContext != null) {

            conversationId = toolContext.getConversationId();

        }

        if (conversationId != null) {

            resolved = merge(resolved, LegalOrchestrationToolContextHolder.getByConversationId(conversationId));

        }



        String sessionId = resolveSessionId(resolved != null ? resolved : toolContext);

        if (StrUtil.isNotBlank(sessionId)) {

            resolved = merge(resolved, LegalOrchestrationToolContextHolder.getBySessionId(sessionId));

            if (resolved == null || resolved.getConversationId() == null) {

                try {

                    Long parsedConversationId = Long.parseLong(sessionId);

                    resolved = merge(resolved,

                            LegalOrchestrationToolContextHolder.getByConversationId(parsedConversationId));

                } catch (NumberFormatException ignored) {

                    // ignore

                }

            }

        }



        return resolved != null ? resolved : toolContext;

    }



    public static LegalOrchestrationToolRuntimeContext resolveFromConversationId(Long conversationId) {

        if (conversationId == null) {

            return null;

        }

        LegalOrchestrationToolRuntimeContext cached =

                LegalOrchestrationToolContextHolder.getByConversationId(conversationId);

        if (cached != null) {

            return cached;

        }

        return LegalOrchestrationToolContextHolder.getBySessionId(String.valueOf(conversationId));

    }



    public static LegalOrchestrationToolRuntimeContext merge(LegalOrchestrationToolRuntimeContext primary,

                                                             LegalOrchestrationToolRuntimeContext fallback) {

        if (primary == null) {

            return fallback;

        }

        if (fallback == null) {

            return primary;

        }

        return LegalOrchestrationToolRuntimeContext.builder()

                .conversationId(primary.getConversationId() != null ? primary.getConversationId() : fallback.getConversationId())

                .userId(primary.getUserId() != null ? primary.getUserId() : fallback.getUserId())

                .tenantId(primary.getTenantId() != null ? primary.getTenantId() : fallback.getTenantId())

                .modelId(primary.getModelId() != null ? primary.getModelId() : fallback.getModelId())

                .build();

    }



    public static Long requireConversationId(LegalOrchestrationToolRuntimeContext toolContext) {

        LegalOrchestrationToolRuntimeContext resolved = resolve(toolContext);

        if (resolved != null && resolved.getConversationId() != null) {

            return resolved.getConversationId();

        }

        String sessionId = resolveSessionId(toolContext);

        if (StrUtil.isNotBlank(sessionId)) {

            try {

                return Long.parseLong(sessionId);

            } catch (NumberFormatException ignored) {

                // ignore

            }

        }

        throw exception(ORCHESTRATION_SESSION_NOT_EXISTS);

    }



    public static Long requireUserId(LegalOrchestrationToolRuntimeContext toolContext) {

        LegalOrchestrationToolRuntimeContext resolved = resolve(toolContext);

        if (resolved != null && resolved.getUserId() != null) {

            return resolved.getUserId();

        }

        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();

        if (loginUserId != null) {

            return loginUserId;

        }

        throw exception(ORCHESTRATION_SESSION_NOT_EXISTS);

    }



    public static Long resolveTenantId(LegalOrchestrationToolRuntimeContext toolContext) {

        LegalOrchestrationToolRuntimeContext resolved = resolve(toolContext);

        if (resolved != null && resolved.getTenantId() != null) {

            return resolved.getTenantId();

        }

        return TenantContextHolder.getTenantId();

    }



    public static LegalOrchestrationToolRuntimeContext fromRuntime(RuntimeContext runtimeContext) {

        if (runtimeContext == null) {

            return null;

        }

        LegalOrchestrationToolRuntimeContext ctx = runtimeContext.get(LegalOrchestrationToolRuntimeContext.class);

        if (ctx != null && ctx.getConversationId() != null) {

            return ctx;

        }

        if (StrUtil.isNotBlank(runtimeContext.getSessionId())) {

            LegalOrchestrationToolRuntimeContext cached =

                    LegalOrchestrationToolContextHolder.getBySessionId(runtimeContext.getSessionId());

            if (cached != null) {

                return merge(ctx, cached);

            }

            try {

                Long conversationId = Long.parseLong(runtimeContext.getSessionId());

                return LegalOrchestrationToolRuntimeContext.builder()

                        .conversationId(conversationId)

                        .userId(parseUserId(runtimeContext.getUserId()))

                        .build();

            } catch (NumberFormatException ignored) {

                return ctx;

            }

        }

        return ctx;

    }



    public static String resolveSessionId(LegalOrchestrationToolRuntimeContext toolContext) {

        if (toolContext != null && toolContext.getConversationId() != null) {

            return String.valueOf(toolContext.getConversationId());

        }

        return null;

    }

    /**
     * 解析编排会话：sessionId 可省略；兼容 Agent 将 conversationId 误传为 sessionId。
     */
    public static LegalOrchestrationSessionDO resolveOrchestrationSession(
            LegalOrchestrationSessionService sessionService,
            Long sessionIdHint,
            LegalOrchestrationToolRuntimeContext toolContext) {
        Long conversationId = requireConversationId(toolContext);
        Long userId = requireUserId(toolContext);
        LegalOrchestrationSessionDO byConversation = sessionService.getByConversationId(conversationId);

        if (sessionIdHint != null) {
            if (byConversation != null && sessionIdHint.equals(byConversation.getId())) {
                assertOrchestrationSessionOwner(byConversation, userId, conversationId);
                return byConversation;
            }
            if (byConversation != null && sessionIdHint.equals(conversationId)) {
                assertOrchestrationSessionOwner(byConversation, userId, conversationId);
                return byConversation;
            }
            LegalOrchestrationSessionDO byId = sessionService.validateSessionExists(sessionIdHint);
            if (userId.equals(byId.getUserId()) && conversationId.equals(byId.getConversationId())) {
                return byId;
            }
            if (byConversation != null && userId.equals(byConversation.getUserId())) {
                return byConversation;
            }
            throw exception(ORCHESTRATION_SESSION_NOT_EXISTS);
        }

        if (byConversation != null && userId.equals(byConversation.getUserId())) {
            return byConversation;
        }
        return sessionService.getOrCreateSession(conversationId, userId,
                toolContext != null ? toolContext.getModelId() : null);
    }

    private static void assertOrchestrationSessionOwner(LegalOrchestrationSessionDO session,
                                                        Long userId, Long conversationId) {
        if (session == null || !userId.equals(session.getUserId())
                || !conversationId.equals(session.getConversationId())) {
            throw exception(ORCHESTRATION_SESSION_NOT_EXISTS);
        }
    }



    private static Long parseUserId(String userId) {

        if (StrUtil.isBlank(userId)) {

            return null;

        }

        try {

            return Long.parseLong(userId);

        } catch (NumberFormatException ignored) {

            return null;

        }

    }



}

