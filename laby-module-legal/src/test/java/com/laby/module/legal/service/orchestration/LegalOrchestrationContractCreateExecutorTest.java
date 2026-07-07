package com.laby.module.legal.service.orchestration;



import com.laby.framework.common.util.json.JsonUtils;

import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;

import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;

import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationFileItemMapper;

import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;

import com.laby.module.legal.enums.contract.LegalContractCreateSourceEnum;

import com.laby.module.legal.enums.orchestration.LegalOrchestrationFileItemStatusEnum;

import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;

import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;

import com.laby.module.legal.service.contract.LegalContractService;

import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;



import java.util.HashMap;

import java.util.List;

import java.util.Map;



import com.laby.framework.common.exception.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)

class LegalOrchestrationContractCreateExecutorTest {



    @InjectMocks

    private LegalOrchestrationContractCreateExecutor executor;



    @Mock

    private LegalOrchestrationSessionService sessionService;

    @Mock

    private LegalOrchestrationFileItemMapper fileItemMapper;

    @Mock

    private LegalContractService contractService;

    @Mock

    private LegalAiPolicyResolver policyResolver;

    @Mock

    private LegalSkillPackSnapshotService skillPackSnapshotService;



    private LegalAiPolicyBO defaultPolicy() {

        return LegalAiPolicyBO.builder()

                .modelId(1L)

                .partyRole(LegalAiPolicyConstants.DEFAULT_PARTY_ROLE)

                .auditLevel(LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL)

                .auditRoleId(101L)

                .policyVersion(LegalAiPolicyConstants.POLICY_VERSION)

                .build();

    }



    @Test

    void executeClassifyConfirm_shouldMapFileItems() {

        Map<String, Object> payload = new HashMap<>();

        payload.put("sessionId", 10L);

        payload.put("mappings", List.of(Map.of("fileItemId", 100L, "typeId", 5L)));

        LegalAgentProposalDO proposal = LegalAgentProposalDO.builder()

                .payloadJson(JsonUtils.toJsonString(payload))

                .build();



        executor.executeClassifyConfirm(proposal);



        ArgumentCaptor<LegalOrchestrationFileItemDO> captor =
                ArgumentCaptor.forClass(LegalOrchestrationFileItemDO.class);

        verify(fileItemMapper).updateById(captor.capture());

        LegalOrchestrationFileItemDO updated = captor.getValue();

        assertEquals(100L, updated.getId());

        assertEquals(5L, updated.getConfirmedTypeId());

        assertEquals(LegalOrchestrationFileItemStatusEnum.MAPPED.getStatus(), updated.getStatus());

        verify(sessionService).updatePhase(10L, "CLASSIFY_CONFIRMED");

    }



    @Test

    void executeCreateContractsBatch_shouldCreateWithAiChatSource() {

        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()

                .id(10L).conversationId(99L).modelId(1L).build();

        LegalOrchestrationFileItemDO file = LegalOrchestrationFileItemDO.builder()

                .id(100L).infraFileId(200L).fileName("采购合同.docx").confirmedTypeId(5L).build();



        Map<String, Object> payload = new HashMap<>();

        payload.put("sessionId", 10L);

        LegalAgentProposalDO proposal = LegalAgentProposalDO.builder()

                .userId(1L)

                .payloadJson(JsonUtils.toJsonString(payload))

                .build();



        when(sessionService.validateSessionExists(10L)).thenReturn(session);

        when(sessionService.listFileItems(10L)).thenReturn(List.of(file));

        LegalAiPolicyBO policy = defaultPolicy();

        when(policyResolver.resolveForExecute(any(), eq(session))).thenReturn(policy);

        when(skillPackSnapshotService.resolveSnapshotForCreate(eq(5L), eq(policy)))

                .thenReturn("{\"audit\":{\"skillPackId\":1}}");

        when(contractService.createContractFromOrchestration(eq(1L), any(), eq(LegalContractCreateSourceEnum.AI_CHAT.getSource()), eq(99L)))

                .thenReturn(1000L);



        List<Long> ids = executor.executeCreateContractsBatch(proposal);



        assertEquals(1, ids.size());

        assertEquals(1000L, ids.get(0));

        ArgumentCaptor<LegalContractCreateReqVO> captor = ArgumentCaptor.forClass(LegalContractCreateReqVO.class);

        verify(contractService).createContractFromOrchestration(eq(1L), captor.capture(),

                eq(LegalContractCreateSourceEnum.AI_CHAT.getSource()), eq(99L));

        assertEquals(5L, captor.getValue().getContractTypeId());

        assertEquals("采购合同", captor.getValue().getTitle());

        assertEquals(101L, captor.getValue().getAuditRoleId());

        verify(sessionService).syncPolicy(10L, defaultPolicy());

    }



    @Test

    void executeCreateContractsBatch_shouldRejectUnconfirmedType() {

        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()

                .id(10L).conversationId(99L).build();

        LegalOrchestrationFileItemDO file = LegalOrchestrationFileItemDO.builder()

                .id(100L).infraFileId(200L).fileName("采购合同.docx").suggestedTypeId(5L).build();



        Map<String, Object> payload = new HashMap<>();

        payload.put("sessionId", 10L);

        LegalAgentProposalDO proposal = LegalAgentProposalDO.builder()

                .userId(1L)

                .payloadJson(JsonUtils.toJsonString(payload))

                .build();



        when(sessionService.validateSessionExists(10L)).thenReturn(session);

        when(sessionService.listFileItems(10L)).thenReturn(List.of(file));

        when(policyResolver.resolveForExecute(any(), eq(session))).thenReturn(defaultPolicy());



        ServiceException ex = assertThrows(ServiceException.class,

                () -> executor.executeCreateContractsBatch(proposal));

        assertEquals(1_050_000_044, ex.getCode());

    }



    @Test

    void executeCreateContractsBatch_shouldApplyPolicyFromResolver() {

        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()

                .id(10L).conversationId(99L).build();

        LegalOrchestrationFileItemDO file = LegalOrchestrationFileItemDO.builder()

                .id(100L).infraFileId(200L).fileName("采购合同.docx").confirmedTypeId(5L).build();



        Map<String, Object> payload = new HashMap<>();

        payload.put("sessionId", 10L);

        LegalAgentProposalDO proposal = LegalAgentProposalDO.builder()

                .userId(1L)

                .payloadJson(JsonUtils.toJsonString(payload))

                .build();



        LegalAiPolicyBO policy = LegalAiPolicyBO.builder()

                .modelId(7L)

                .partyRole("B")

                .auditLevel("strict")

                .auditRoleId(200L)

                .build();



        when(sessionService.validateSessionExists(10L)).thenReturn(session);

        when(sessionService.listFileItems(10L)).thenReturn(List.of(file));

        when(policyResolver.resolveForExecute(any(), eq(session))).thenReturn(policy);

        when(skillPackSnapshotService.resolveSnapshotForCreate(eq(5L), eq(policy)))

                .thenReturn("{\"audit\":{\"skillPackId\":2}}");

        when(contractService.createContractFromOrchestration(eq(1L), any(), eq(LegalContractCreateSourceEnum.AI_CHAT.getSource()), eq(99L)))

                .thenReturn(1000L);



        executor.executeCreateContractsBatch(proposal);



        ArgumentCaptor<LegalContractCreateReqVO> captor = ArgumentCaptor.forClass(LegalContractCreateReqVO.class);

        verify(contractService).createContractFromOrchestration(eq(1L), captor.capture(),

                eq(LegalContractCreateSourceEnum.AI_CHAT.getSource()), eq(99L));

        assertEquals("{\"audit\":{\"skillPackId\":2}}", captor.getValue().getSkillPackSnapshotJson());

        assertEquals(7L, captor.getValue().getModelId());

        assertEquals("B", captor.getValue().getPartyRole());

        assertEquals("strict", captor.getValue().getAuditLevel());

        assertEquals(200L, captor.getValue().getAuditRoleId());

    }



}

