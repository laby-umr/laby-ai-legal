package com.laby.module.legal.service.orchestration;

import com.laby.framework.common.exception.ServiceException;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;
import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;
import com.laby.module.legal.service.ai.kernel.LegalAuditKernel;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelResult;
import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalOrchestrationPreviewAuditServiceTest {

    @InjectMocks
    private LegalOrchestrationPreviewAuditService previewAuditService;

    @Mock
    private LegalOrchestrationSessionService sessionService;
    @Mock
    private LegalOrchestrationSessionMapper sessionMapper;
    @Mock
    private LegalAiPolicyResolver policyResolver;
    @Mock
    private LegalAuditKernel auditKernel;
    @Mock
    private LegalSkillPackSnapshotService skillPackSnapshotService;

    private LegalAiPolicyBO defaultPolicy() {
        return LegalAiPolicyBO.builder()
                .modelId(1L)
                .partyRole(LegalAiPolicyConstants.DEFAULT_PARTY_ROLE)
                .auditLevel(LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL)
                .policyVersion(LegalAiPolicyConstants.POLICY_VERSION)
                .build();
    }

    @Test
    void preview_shouldRejectSuggestedTypeWithoutConfirmation() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(10L).modelId(1L).build();
        LegalOrchestrationFileItemDO file = LegalOrchestrationFileItemDO.builder()
                .id(100L).infraFileId(200L).fileName("采购合同.docx")
                .suggestedTypeId(5L).build();

        when(sessionService.validateSessionExists(10L)).thenReturn(session);
        when(policyResolver.resolveForSession(eq(session), eq(null), eq(null), eq(null)))
                .thenReturn(defaultPolicy());
        when(sessionService.listFileItems(10L)).thenReturn(List.of(file));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> previewAuditService.preview(10L, null, null));
        assertEquals(1_050_000_044, ex.getCode());
    }

    @Test
    void preview_shouldUseConfirmedTypeId() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(10L).modelId(1L).build();
        LegalOrchestrationFileItemDO file = LegalOrchestrationFileItemDO.builder()
                .id(100L).infraFileId(200L).fileName("采购合同.docx")
                .confirmedTypeId(5L).build();
        LegalAiPolicyBO policy = defaultPolicy();

        when(sessionService.validateSessionExists(10L)).thenReturn(session);
        when(policyResolver.resolveForSession(eq(session), eq(null), eq(null), eq(null)))
                .thenReturn(policy);
        when(sessionService.listFileItems(10L)).thenReturn(List.of(file));
        when(auditKernel.runPreview(any())).thenReturn(LegalAuditKernelResult.builder()
                .items(List.of())
                .build());

        previewAuditService.preview(10L, null, null);

        verify(skillPackSnapshotService).freezeSnapshotOnPolicy(policy, 5L);
        verify(auditKernel).runPreview(any());
        verify(sessionMapper).updateById(any(LegalOrchestrationSessionDO.class));
    }

}
