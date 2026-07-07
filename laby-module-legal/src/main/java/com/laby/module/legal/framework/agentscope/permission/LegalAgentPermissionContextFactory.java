package com.laby.module.legal.framework.agentscope.permission;

import com.laby.module.legal.service.agent.LegalAgentToolProvider;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;

/**
 * 法务合同 Agent Permission：只读 Tool 自动 ALLOW，写操作 Tool 走 ASK。
 * <p>
 * AgentScope {@link PermissionMode#DEFAULT} 对未匹配规则的工具默认 ASK，因此提案模式下须显式 ALLOW 只读 Tool。
 */
public final class LegalAgentPermissionContextFactory {

    private static final String SOURCE = "legal";

    public static final String TOOL_PROPOSE_ADOPT = "legal_propose_adopt_opinion";
    public static final String TOOL_ADOPT = "legal_adopt_opinion";
    public static final String TOOL_BATCH_ADOPT = "legal_batch_adopt_pending_opinions";
    public static final String TOOL_PROPOSE_SKIP = "legal_propose_skip_paragraph";

    private LegalAgentPermissionContextFactory() {
    }

    public static PermissionContextState build(boolean allowProposal) {
        PermissionContextState.Builder builder = PermissionContextState.builder();
        if (allowProposal) {
            builder.mode(PermissionMode.DEFAULT);
            for (String toolName : LegalAgentToolProvider.getReadonlyToolNames()) {
                builder.addAllowRule(toolName, allowRule(toolName));
            }
            // 用户对话即授权，写操作 Tool 直接 ALLOW，不打断 SSE
            builder.addAllowRule(TOOL_PROPOSE_ADOPT, allowRule(TOOL_PROPOSE_ADOPT))
                    .addAllowRule(TOOL_ADOPT, allowRule(TOOL_ADOPT))
                    .addAllowRule(TOOL_BATCH_ADOPT, allowRule(TOOL_BATCH_ADOPT))
                    .addAllowRule(TOOL_PROPOSE_SKIP, allowRule(TOOL_PROPOSE_SKIP));
            // Harness 内置检索工具，DEFAULT 模式下未声明会 ASK 导致误弹确认框
            builder.addAllowRule("session_search", allowRule("session_search"));
        } else {
            builder.mode(PermissionMode.EXPLORE)
                    .addDenyRule(TOOL_PROPOSE_ADOPT, denyRule(TOOL_PROPOSE_ADOPT))
                    .addDenyRule(TOOL_PROPOSE_SKIP, denyRule(TOOL_PROPOSE_SKIP));
        }
        return builder.build();
    }

    private static PermissionRule allowRule(String toolName) {
        return new PermissionRule(toolName, null, PermissionBehavior.ALLOW, SOURCE);
    }

    private static PermissionRule askRule(String toolName) {
        return new PermissionRule(toolName, null, PermissionBehavior.ASK, SOURCE);
    }

    private static PermissionRule denyRule(String toolName) {
        return new PermissionRule(toolName, null, PermissionBehavior.DENY, SOURCE);
    }

}
