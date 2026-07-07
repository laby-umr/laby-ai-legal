package com.laby.module.legal.integration;

import com.laby.framework.common.exception.ServiceException;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.framework.security.core.LoginUser;
import com.laby.framework.test.core.ut.BaseDbUnitTest;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.agent.LegalAgentProposalMapper;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationFileItemMapper;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;
import com.laby.module.legal.enums.agent.LegalAgentProposalActionEnum;
import com.laby.module.legal.enums.agent.LegalAgentProposalStatusEnum;
import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;
import com.laby.module.legal.enums.contract.LegalContractCreateSourceEnum;
import com.laby.module.legal.enums.orchestration.LegalOrchestrationFileItemStatusEnum;
import com.laby.module.legal.service.agent.LegalAgentProposalService;
import com.laby.module.legal.service.agent.LegalAgentStepLogService;
import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.contract.LegalContractAuditRoleService;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.opinion.LegalAuditOpinionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationCheckpointService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationContractCreateExecutor;
import com.laby.module.legal.dal.dataobject.skillpack.LegalSkillPackDO;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionServiceImpl;
import com.laby.module.legal.service.orchestration.LegalOrchestrationTypePackageService;
import com.laby.module.legal.service.skillpack.LegalSkillPackRegistry;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.ai.service.chat.AiChatConversationService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 编排三链路集成测试：Policy 快照持久化、提案鉴权、Policy→创建合同参数传递。
 */
@Import({
        LegalAiPolicyResolver.class,
        LegalSkillPackSnapshotService.class,
        LegalOrchestrationSessionServiceImpl.class,
        LegalOrchestrationCheckpointService.class,
        LegalOrchestrationContractCreateExecutor.class,
        LegalAgentProposalService.class,
})
class LegalOrchestrationIntegrationIT extends BaseDbUnitTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final Long CONVERSATION_ID = 999L;
    private static final Long MODEL_ID = 7L;
    private static final Long TYPE_ID = 5L;
    @Resource
    private LegalAiPolicyResolver policyResolver;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;
    @Resource
    private LegalOrchestrationSessionServiceImpl sessionService;
    @Resource
    private LegalOrchestrationSessionMapper sessionMapper;
    @Resource
    private LegalOrchestrationFileItemMapper fileItemMapper;
    @Resource
    private LegalOrchestrationContractCreateExecutor contractCreateExecutor;
    @Resource
    private LegalAgentProposalMapper proposalMapper;
    @Resource
    private LegalAgentProposalService proposalService;

    @MockitoBean
    private AiChatConversationService aiChatConversationService;
    @MockitoBean
    private LegalContractAuditRoleService auditRoleService;
    @MockitoBean
    private LegalSkillPackRegistry skillPackRegistry;
    @MockitoBean
    private LegalContractService contractService;
    @MockitoBean
    private LegalOrchestrationTypePackageService orchestrationTypePackageService;
    @MockitoBean
    private LegalAuditOpinionService opinionService;
    @MockitoBean
    private LegalAgentStepLogService agentStepLogService;

    @BeforeEach
    void setUpSecurity() {
        when(auditRoleService.resolveDefaultRound1RoleId()).thenReturn(101L);
        when(skillPackRegistry.sanitizeToolNames(any())).thenReturn(java.util.List.of());
        LoginUser loginUser = new LoginUser().setId(USER_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, "token"));
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void policySnapshot_shouldRoundTripThroughSessionAndExecute() {
        LegalOrchestrationSessionDO session = insertSession();
        LegalAiPolicyBO policy = defaultPolicy();
        skillPackSnapshotService.freezeSnapshotOnPolicy(policy, TYPE_ID);
        String frozenSnapshot = policy.getSkillPackSnapshotJson();
        sessionService.syncPolicy(session.getId(), policy);

        LegalOrchestrationSessionDO reloaded = sessionMapper.selectById(session.getId());
        LegalAiPolicyBO resolved = policyResolver.resolveForExecute(Map.of("sessionId", session.getId()), reloaded);

        assertEquals(frozenSnapshot, resolved.getSkillPackSnapshotJson());
        assertEquals(TYPE_ID, resolved.getSkillPackSnapshotContractTypeId());
    }

    @Test
    void createContractsBatch_shouldPassFrozenSnapshotToContractService() {
        LegalOrchestrationSessionDO session = insertSession();
        insertFileItem(session.getId());
        LegalAiPolicyBO policy = defaultPolicy();
        skillPackSnapshotService.freezeSnapshotOnPolicy(policy, TYPE_ID);
        String frozenSnapshot = policy.getSkillPackSnapshotJson();
        sessionService.syncPolicy(session.getId(), policy);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", session.getId());
        LegalAgentProposalDO proposal = LegalAgentProposalDO.builder()
                .userId(USER_ID)
                .payloadJson(JsonUtils.toJsonString(payload))
                .build();

        when(contractService.createContractFromOrchestration(eq(USER_ID), any(),
                eq(LegalContractCreateSourceEnum.AI_CHAT.getSource()), eq(CONVERSATION_ID)))
                .thenReturn(1000L);

        contractCreateExecutor.executeCreateContractsBatch(proposal);

        ArgumentCaptor<LegalContractCreateReqVO> captor = ArgumentCaptor.forClass(LegalContractCreateReqVO.class);
        verify(contractService).createContractFromOrchestration(eq(USER_ID), captor.capture(),
                eq(LegalContractCreateSourceEnum.AI_CHAT.getSource()), eq(CONVERSATION_ID));
        assertEquals(frozenSnapshot, captor.getValue().getSkillPackSnapshotJson());
        assertEquals(TYPE_ID, captor.getValue().getContractTypeId());
    }

    @Test
    void executeProposal_shouldRejectWhenOrchestrationSessionNotOwned() {
        sessionMapper.insert(LegalOrchestrationSessionDO.builder()
                .conversationId(CONVERSATION_ID)
                .userId(OTHER_USER_ID)
                .phase("INIT")
                .modelId(MODEL_ID)
                .build());
        proposalMapper.insert(LegalAgentProposalDO.builder()
                .proposalNo("it-proposal-deny")
                .conversationId(CONVERSATION_ID)
                .userId(USER_ID)
                .sessionId("sess-it")
                .action(LegalAgentProposalActionEnum.CLASSIFY_CONFIRM.getAction())
                .status(LegalAgentProposalStatusEnum.PENDING.getStatus())
                .title("分类确认")
                .payloadJson("{}")
                .expireTime(LocalDateTime.now().plusMinutes(5))
                .build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> proposalService.executeProposal("it-proposal-deny"));
        assertEquals(1_050_000_040, ex.getCode());
    }

    private LegalOrchestrationSessionDO insertSession() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .conversationId(CONVERSATION_ID)
                .userId(USER_ID)
                .phase("INIT")
                .modelId(MODEL_ID)
                .partyRole(LegalAiPolicyConstants.DEFAULT_PARTY_ROLE)
                .auditLevel(LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL)
                .auditRoleId(101L)
                .build();
        sessionMapper.insert(session);
        return session;
    }

    private void insertFileItem(Long sessionId) {
        fileItemMapper.insert(LegalOrchestrationFileItemDO.builder()
                .sessionId(sessionId)
                .infraFileId(200L)
                .fileName("采购合同.docx")
                .status(LegalOrchestrationFileItemStatusEnum.MAPPED.getStatus())
                .confirmedTypeId(TYPE_ID)
                .sort(0)
                .build());
    }

    private LegalAiPolicyBO defaultPolicy() {
        mockSkillPackRegistry();
        return LegalAiPolicyBO.builder()
                .modelId(MODEL_ID)
                .partyRole(LegalAiPolicyConstants.DEFAULT_PARTY_ROLE)
                .auditLevel(LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL)
                .auditRoleId(101L)
                .policyVersion(LegalAiPolicyConstants.POLICY_VERSION)
                .build();
    }

    private void mockSkillPackRegistry() {
        LegalSkillPackDO auditPack = LegalSkillPackDO.builder()
                .id(42L)
                .code("audit-pack")
                .version(1)
                .scene(LegalSkillPackSceneEnum.AUDIT.getCode())
                .toolNames("[]")
                .build();
        when(skillPackRegistry.resolveForContractType(TYPE_ID, LegalSkillPackSceneEnum.AUDIT.getCode()))
                .thenReturn(java.util.Optional.of(auditPack));
        when(skillPackRegistry.resolveForContractType(TYPE_ID, LegalSkillPackSceneEnum.CHAT.getCode()))
                .thenReturn(java.util.Optional.empty());
        when(skillPackRegistry.sanitizeToolNames(any())).thenReturn(java.util.List.of());
    }

}
