package com.laby.module.legal.service.ai.policy;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.ai.service.chat.AiChatConversationService;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;
import com.laby.module.legal.enums.contract.LegalAuditLevelEnum;
import com.laby.module.legal.enums.contract.LegalPartyRoleEnum;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.contract.LegalContractAuditRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_POLICY_INVALID;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_POLICY_MODEL_MISSING;

/**
 * 法务 AI 策略解析器：三链路唯一 Policy 来源
 */
@Service
public class LegalAiPolicyResolver {

    @Resource
    private AiChatConversationService aiChatConversationService;
    @Resource
    private LegalContractAuditRoleService auditRoleService;

    /**
     * 编排 Harness 初始化：从对话解析基础策略
     */
    public LegalAiPolicyBO resolveForConversation(Long conversationId, Long modelIdHint) {
        Long modelId = resolveModelId(modelIdHint, null, conversationId);
        return buildPolicy(conversationId, modelId,
                LegalAiPolicyConstants.DEFAULT_PARTY_ROLE,
                LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL,
                null, null);
    }

    /**
     * 创建合同提案：合并 Tool 参数与会话已绑定字段
     */
    public LegalAiPolicyBO resolveForSession(LegalOrchestrationSessionDO session, Long modelIdHint,
                                              String partyRoleOverride, String auditLevelOverride) {
        Long conversationId = session != null ? session.getConversationId() : null;
        Long modelId = resolveModelId(modelIdHint,
                session != null ? session.getModelId() : null,
                conversationId);
        String partyRole = normalizePartyRole(StrUtil.blankToDefault(partyRoleOverride,
                session != null ? session.getPartyRole() : null));
        String auditLevel = normalizeAuditLevel(StrUtil.blankToDefault(auditLevelOverride,
                session != null ? session.getAuditLevel() : null));
        Long auditRoleId = session != null && session.getAuditRoleId() != null
                ? session.getAuditRoleId()
                : auditRoleService.resolveDefaultRound1RoleId();
        return buildPolicy(conversationId, modelId, partyRole, auditLevel, auditRoleId, null);
    }

    /**
     * 执行创建合同提案：payload &gt; session &gt; conversation
     */
    public LegalAiPolicyBO resolveForExecute(Map<?, ?> payload, LegalOrchestrationSessionDO session) {
        Long conversationId = session.getConversationId();
        Long modelId = extractLong(payload, "modelId");
        modelId = resolveModelId(modelId, session.getModelId(), conversationId);

        String partyRole = normalizePartyRole(StrUtil.blankToDefault(extractString(payload, "partyRole"),
                session.getPartyRole()));
        String auditLevel = normalizeAuditLevel(StrUtil.blankToDefault(extractString(payload, "auditLevel"),
                session.getAuditLevel()));
        Long auditRoleId = extractLong(payload, "auditRoleId");
        if (auditRoleId == null) {
            auditRoleId = session.getAuditRoleId();
        }
        if (auditRoleId == null) {
            auditRoleId = auditRoleService.resolveDefaultRound1RoleId();
        }
        Long reauditRoleId = extractLong(payload, "reauditRoleId");
        LegalAiPolicyBO policy = buildPolicy(conversationId, modelId, partyRole, auditLevel, auditRoleId, reauditRoleId);
        mergeSkillPackSnapshotFromPayload(policy, payload);
        mergeSkillPackSnapshotFromSession(policy, session);
        return policy;
    }

    /**
     * 转为提案 payload 字段
     */
    public Map<String, Object> toPayloadMap(LegalAiPolicyBO policy) {
        Map<String, Object> payload = new HashMap<>(8);
        payload.put("modelId", policy.getModelId());
        payload.put("partyRole", policy.getPartyRole());
        payload.put("auditLevel", policy.getAuditLevel());
        payload.put("auditRoleId", policy.getAuditRoleId());
        if (policy.getReauditRoleId() != null) {
            payload.put("reauditRoleId", policy.getReauditRoleId());
        }
        payload.put("policyVersion", policy.getPolicyVersion());
        if (StrUtil.isNotBlank(policy.getSkillPackSnapshotJson())) {
            payload.put("skillPackSnapshotJson", policy.getSkillPackSnapshotJson());
        }
        if (policy.getSkillPackSnapshotContractTypeId() != null) {
            payload.put("skillPackSnapshotContractTypeId", policy.getSkillPackSnapshotContractTypeId());
        }
        return payload;
    }

    public String normalizePartyRole(String raw) {
        if (StrUtil.isBlank(raw)) {
            return LegalAiPolicyConstants.DEFAULT_PARTY_ROLE;
        }
        for (LegalPartyRoleEnum item : LegalPartyRoleEnum.values()) {
            if (item.getCode().equalsIgnoreCase(raw.trim())) {
                return item.getCode();
            }
        }
        throw exception(ORCHESTRATION_POLICY_INVALID);
    }

    public String normalizeAuditLevel(String raw) {
        if (StrUtil.isBlank(raw)) {
            return LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL;
        }
        for (LegalAuditLevelEnum item : LegalAuditLevelEnum.values()) {
            if (item.getCode().equalsIgnoreCase(raw.trim())) {
                return item.getCode();
            }
        }
        throw exception(ORCHESTRATION_POLICY_INVALID);
    }

    private LegalAiPolicyBO buildPolicy(Long conversationId, Long modelId,
                                        String partyRole, String auditLevel,
                                        Long auditRoleId, Long reauditRoleId) {
        validateModelId(modelId);
        if (auditRoleId == null) {
            auditRoleId = auditRoleService.resolveDefaultRound1RoleId();
        }
        return LegalAiPolicyBO.builder()
                .conversationId(conversationId)
                .modelId(modelId)
                .partyRole(normalizePartyRole(partyRole))
                .auditLevel(normalizeAuditLevel(auditLevel))
                .auditRoleId(auditRoleId)
                .reauditRoleId(reauditRoleId)
                .policyVersion(LegalAiPolicyConstants.POLICY_VERSION)
                .build();
    }

    private Long resolveModelId(Long primary, Long sessionModelId, Long conversationId) {
        if (primary != null) {
            return primary;
        }
        if (sessionModelId != null) {
            return sessionModelId;
        }
        if (conversationId != null) {
            AiChatConversationDO conversation = aiChatConversationService.getChatConversation(conversationId);
            if (conversation != null && conversation.getModelId() != null) {
                return conversation.getModelId();
            }
        }
        throw exception(ORCHESTRATION_POLICY_MODEL_MISSING);
    }

    private void validateModelId(Long modelId) {
        if (modelId == null) {
            throw exception(ORCHESTRATION_POLICY_MODEL_MISSING);
        }
    }

    private static Long extractLong(Map<?, ?> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static String extractString(Map<?, ?> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private static void mergeSkillPackSnapshotFromPayload(LegalAiPolicyBO policy, Map<?, ?> payload) {
        if (payload == null) {
            return;
        }
        String snapshotJson = extractString(payload, "skillPackSnapshotJson");
        if (StrUtil.isNotBlank(snapshotJson)) {
            policy.setSkillPackSnapshotJson(snapshotJson);
        }
        Long typeId = extractLong(payload, "skillPackSnapshotContractTypeId");
        if (typeId != null) {
            policy.setSkillPackSnapshotContractTypeId(typeId);
        }
    }

    private static void mergeSkillPackSnapshotFromSession(LegalAiPolicyBO policy, LegalOrchestrationSessionDO session) {
        if (session == null || StrUtil.isBlank(session.getPolicyJson())) {
            return;
        }
        LegalAiPolicyBO sessionPolicy = JsonUtils.parseObject(session.getPolicyJson(), LegalAiPolicyBO.class);
        if (sessionPolicy == null) {
            return;
        }
        if (StrUtil.isBlank(policy.getSkillPackSnapshotJson()) && StrUtil.isNotBlank(sessionPolicy.getSkillPackSnapshotJson())) {
            policy.setSkillPackSnapshotJson(sessionPolicy.getSkillPackSnapshotJson());
        }
        if (policy.getSkillPackSnapshotContractTypeId() == null && sessionPolicy.getSkillPackSnapshotContractTypeId() != null) {
            policy.setSkillPackSnapshotContractTypeId(sessionPolicy.getSkillPackSnapshotContractTypeId());
        }
    }

}
