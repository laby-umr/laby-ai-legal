package com.laby.module.ai.framework.agentscope.session;

import cn.hutool.core.util.StrUtil;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;

/**
 * HarnessAgent SessionKey 构建（多租户 / 多业务隔离）。
 */
public final class AgentScopeSessionKeyBuilder {

    private AgentScopeSessionKeyBuilder() {
    }

    public static SessionKey legalContract(String keyPrefix, Long contractId, String sessionId) {
        return SimpleSessionKey.of(join(keyPrefix, "legal", String.valueOf(contractId), sessionId));
    }

    public static SessionKey aiChat(String keyPrefix, String conversationId) {
        return SimpleSessionKey.of(join(keyPrefix, "chat", conversationId));
    }

    private static String join(String prefix, String... parts) {
        // WorkspaceSession 将 toIdentifier() 直接作为目录名；Windows 路径不允许 ':'。
        String normalizedPrefix = StrUtil.blankToDefault(prefix, "as:");
        normalizedPrefix = normalizedPrefix.replace(':', '_');
        if (!normalizedPrefix.endsWith("_")) {
            normalizedPrefix = normalizedPrefix + "_";
        }
        return normalizedPrefix + String.join("_", parts);
    }

}
