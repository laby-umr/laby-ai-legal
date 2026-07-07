package com.laby.module.legal.enums;

import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;

/**
 * 法务 AI 编排常量
 */
public final class LegalOrchestrationConstants {

    /** AI 聊天角色 id（与 ai_chat_role.id 一致） */
    public static final long ORCHESTRATION_ROLE_ID = 123L;

    /** AI 聊天角色编码 */
    public static final String ORCHESTRATION_ROLE_CODE = "legal_orchestration";

    /** 编排 Tool Bean 名前缀 */
    public static final String ORCHESTRATION_TOOL_PREFIX = "legal_orchestration_";

    /** 编排提案有效期（分钟） */
    public static final int PROPOSAL_TTL_MINUTES = 30;

    /** 默认审核强度（委托 Policy 常量） */
    public static final String DEFAULT_AUDIT_LEVEL = LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL;

    /** 默认我方立场（委托 Policy 常量） */
    public static final String DEFAULT_PARTY_ROLE = LegalAiPolicyConstants.DEFAULT_PARTY_ROLE;

    private LegalOrchestrationConstants() {
    }

}
