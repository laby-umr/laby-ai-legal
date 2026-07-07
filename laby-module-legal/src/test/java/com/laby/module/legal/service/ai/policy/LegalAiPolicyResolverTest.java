package com.laby.module.legal.service.ai.policy;

import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.laby.module.ai.service.chat.AiChatConversationService;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;
import com.laby.module.legal.enums.contract.LegalAuditLevelEnum;
import com.laby.module.legal.enums.contract.LegalPartyRoleEnum;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.contract.LegalContractAuditRoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalAiPolicyResolverTest {

    @InjectMocks
    private LegalAiPolicyResolver policyResolver;

    @Mock
    private AiChatConversationService aiChatConversationService;
    @Mock
    private LegalContractAuditRoleService auditRoleService;

    @Test
    void resolveForConversation_shouldUseConversationModel() {
        when(aiChatConversationService.getChatConversation(99L))
                .thenReturn(AiChatConversationDO.builder().id(99L).modelId(7L).build());
        when(auditRoleService.resolveDefaultRound1RoleId()).thenReturn(101L);

        LegalAiPolicyBO policy = policyResolver.resolveForConversation(99L, null);

        assertEquals(7L, policy.getModelId());
        assertEquals(LegalPartyRoleEnum.PARTY_A.getCode(), policy.getPartyRole());
        assertEquals(LegalAuditLevelEnum.STANDARD.getCode(), policy.getAuditLevel());
        assertEquals(101L, policy.getAuditRoleId());
        assertEquals(LegalAiPolicyConstants.POLICY_VERSION, policy.getPolicyVersion());
    }

    @Test
    void resolveForSession_shouldMergeOverrides() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(1L).conversationId(99L).modelId(7L).partyRole("A").auditLevel("standard").build();
        when(auditRoleService.resolveDefaultRound1RoleId()).thenReturn(101L);

        LegalAiPolicyBO policy = policyResolver.resolveForSession(session, null, "B", "strict");

        assertEquals(7L, policy.getModelId());
        assertEquals(LegalPartyRoleEnum.PARTY_B.getCode(), policy.getPartyRole());
        assertEquals(LegalAuditLevelEnum.STRICT.getCode(), policy.getAuditLevel());
    }

    @Test
    void resolveForExecute_shouldPreferPayload() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(10L).conversationId(99L).modelId(1L).build();
        Map<String, Object> payload = new HashMap<>();
        payload.put("modelId", 7L);
        payload.put("partyRole", "B");
        payload.put("auditLevel", "relaxed");
        payload.put("auditRoleId", 200L);

        LegalAiPolicyBO policy = policyResolver.resolveForExecute(payload, session);

        assertEquals(7L, policy.getModelId());
        assertEquals(LegalPartyRoleEnum.PARTY_B.getCode(), policy.getPartyRole());
        assertEquals(LegalAuditLevelEnum.RELAXED.getCode(), policy.getAuditLevel());
        assertEquals(200L, policy.getAuditRoleId());
    }

    @Test
    void normalizePartyRole_shouldRejectInvalid() {
        assertThrows(Exception.class, () -> policyResolver.normalizePartyRole("INVALID"));
    }

    @Test
    void resolveForExecute_shouldMergeSkillPackSnapshotFromSession() {
        LegalAiPolicyBO sessionPolicy = LegalAiPolicyBO.builder()
                .modelId(7L)
                .skillPackSnapshotJson("{\"audit\":{\"skillPackId\":42}}")
                .skillPackSnapshotContractTypeId(5L)
                .build();
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(10L).conversationId(99L).modelId(7L)
                .policyJson(JsonUtils.toJsonString(sessionPolicy))
                .build();
        when(auditRoleService.resolveDefaultRound1RoleId()).thenReturn(101L);

        LegalAiPolicyBO policy = policyResolver.resolveForExecute(Map.of(), session);

        assertEquals("{\"audit\":{\"skillPackId\":42}}", policy.getSkillPackSnapshotJson());
        assertEquals(5L, policy.getSkillPackSnapshotContractTypeId());
    }

    @Test
    void resolveForConversation_shouldThrowWhenModelMissing() {
        when(aiChatConversationService.getChatConversation(99L))
                .thenReturn(AiChatConversationDO.builder().id(99L).build());

        assertThrows(Exception.class, () -> policyResolver.resolveForConversation(99L, null));
    }

}
