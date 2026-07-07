package com.laby.module.legal.tool.agent;

import com.laby.framework.security.core.LoginUser;
import com.laby.module.ai.util.AiUtils;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * AgentScope RuntimeContext 注入的法务 Tool 上下文（替代 Spring AI ToolContext）。
 */
@Data
@Builder
public class LegalAgentToolRuntimeContext {

    private Long contractId;
    private String sessionId;
    private boolean readonly;
    private Long tenantId;
    private LoginUser loginUser;

    public static LegalAgentToolRuntimeContext from(Map<String, Object> context) {
        if (context == null) {
            return LegalAgentToolRuntimeContext.builder().readonly(true).build();
        }
        Long contractId = parseLong(context.get(LegalAgentToolContext.CONTRACT_ID));
        String sessionId = context.get(LegalAgentToolContext.SESSION_ID) != null
                ? String.valueOf(context.get(LegalAgentToolContext.SESSION_ID)) : null;
        boolean readonly = context.get(LegalAgentToolContext.READONLY) instanceof Boolean b && b;
        Long tenantId = parseLong(context.get(AiUtils.TOOL_CONTEXT_TENANT_ID));
        LoginUser loginUser = context.get(AiUtils.TOOL_CONTEXT_LOGIN_USER) instanceof LoginUser user ? user : null;
        return LegalAgentToolRuntimeContext.builder()
                .contractId(contractId)
                .sessionId(sessionId)
                .readonly(readonly)
                .tenantId(tenantId)
                .loginUser(loginUser)
                .build();
    }

    private static Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

}
