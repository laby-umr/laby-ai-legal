package com.laby.module.legal.service.agent;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.security.core.LoginUser;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 步骤日志 / Tool 步数上下文（按 sessionId 存储，Tool 异步线程可绑定）。
 */
public final class LegalAgentStepLogContext {

    private static final ThreadLocal<State> STATE = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, State> SESSIONS = new ConcurrentHashMap<>();

    private LegalAgentStepLogContext() {
    }

    public static void init(Long contractId, String sessionId, Long userId) {
        init(contractId, sessionId, userId, false, null, null);
    }

    public static void init(Long contractId, String sessionId, Long userId,
                            boolean allowProposal, Long tenantId, LoginUser loginUser) {
        State state = new State();
        state.setContractId(contractId);
        state.setSessionId(sessionId);
        state.setUserId(userId);
        state.setAllowProposal(allowProposal);
        state.setTenantId(tenantId);
        state.setLoginUser(loginUser);
        state.setStepIndex(0);
        state.setToolCallCount(0);
        state.setToolStepLimitReached(false);
        SESSIONS.put(sessionId, state);
        STATE.set(state);
    }

    public static void bindSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }
        State state = SESSIONS.get(sessionId);
        if (state != null) {
            STATE.set(state);
        }
    }

    public static State getSessionState(String sessionId) {
        return StrUtil.isBlank(sessionId) ? null : SESSIONS.get(sessionId);
    }

    public static void unbindThread() {
        STATE.remove();
    }

    public static void removeSession(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            SESSIONS.remove(sessionId);
            LegalAgentToolProvider.removeSessionToolContext(sessionId);
        }
        STATE.remove();
    }

    public static State getState() {
        return STATE.get();
    }

    public static boolean isActive() {
        return STATE.get() != null;
    }

    public static int nextStepIndex() {
        State state = STATE.get();
        if (state == null) {
            return 0;
        }
        state.setStepIndex(state.getStepIndex() + 1);
        return state.getStepIndex();
    }

    public static int incrementToolCallCount() {
        State state = STATE.get();
        if (state == null) {
            return 0;
        }
        state.setToolCallCount(state.getToolCallCount() + 1);
        return state.getToolCallCount();
    }

    public static boolean isToolStepLimitReached() {
        State state = STATE.get();
        return state != null && state.isToolStepLimitReached();
    }

    public static void markToolStepLimitReached() {
        State state = STATE.get();
        if (state != null) {
            state.setToolStepLimitReached(true);
        }
    }

    @Data
    public static class State {
        private Long contractId;
        private String sessionId;
        private Long userId;
        private boolean allowProposal;
        private Long tenantId;
        private LoginUser loginUser;
        private int stepIndex;
        private int toolCallCount;
        private boolean toolStepLimitReached;
    }

}
