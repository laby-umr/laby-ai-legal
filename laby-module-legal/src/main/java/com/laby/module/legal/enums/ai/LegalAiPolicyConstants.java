package com.laby.module.legal.enums.ai;

import com.laby.module.legal.enums.contract.LegalAuditLevelEnum;
import com.laby.module.legal.enums.contract.LegalPartyRoleEnum;

/**
 * 法务 AI 策略常量（三链路统一 Policy 层）
 */
public final class LegalAiPolicyConstants {

    /** 策略快照版本号 */
    public static final String POLICY_VERSION = "2026-06-08-v1";

    /** 默认我方立场 */
    public static final String DEFAULT_PARTY_ROLE = LegalPartyRoleEnum.PARTY_A.getCode();

    /** 默认审核强度 */
    public static final String DEFAULT_AUDIT_LEVEL = LegalAuditLevelEnum.STANDARD.getCode();

    private LegalAiPolicyConstants() {
    }

}
