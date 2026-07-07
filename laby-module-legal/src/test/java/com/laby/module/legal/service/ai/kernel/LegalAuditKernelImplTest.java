package com.laby.module.legal.service.ai.kernel;

import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelCommand;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditPreviewCommand;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.ai.LegalAiModelResolver;
import com.laby.module.legal.service.contract.LegalContractAuditRoleService;
import com.laby.module.legal.service.orchestrator.LegalAiOrchestrator;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAuditOrchestrationResult;
import com.laby.module.legal.service.playbook.LegalDeterministicAuditEngine;
import com.laby.module.legal.service.playbook.LegalReviewPlanCompiler;
import com.laby.module.legal.service.skillpack.LegalSkillPackRegistry;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalAuditKernelImplTest {

    @InjectMocks
    private LegalAuditKernelImpl auditKernel;

    @Mock
    private AiModelService aiModelService;
    @Mock
    private LegalAiModelResolver legalAiModelResolver;
    @Mock
    private LegalContractAuditRoleService auditRoleService;
    @Mock
    private LegalAiOrchestrator legalAiOrchestrator;
    @Mock
    private LegalReviewPlanCompiler reviewPlanCompiler;
    @Mock
    private LegalDeterministicAuditEngine deterministicAuditEngine;
    @Mock
    private LegalAuditPreviewParseService previewParseService;
    @Mock
    private LegalSkillPackRegistry skillPackRegistry;
    @Mock
    private LegalSkillPackSnapshotService skillPackSnapshotService;

    @Test
    void runFormal_shouldMergePlaybookAndLlm() {
        LegalContractDO contract = LegalContractDO.builder()
                .id(1L).contractTypeId(5L).modelId(7L).partyRole("A").auditLevel("standard").build();
        LegalContractParagraphDO paragraph = LegalContractParagraphDO.builder()
                .paragraphId("p-1").text("test").build();
        AiModelDO model = AiModelDO.builder().id(7L).maxTokens(4096).build();

        LegalAiAuditOpinionItemBO aiItem = new LegalAiAuditOpinionItemBO();
        aiItem.setTitle("LLM 意见");
        aiItem.setSourceType(LegalOpinionSourceTypeEnum.AI.getCode());

        when(legalAiModelResolver.requireChatModel(7L)).thenReturn(model);
        when(auditRoleService.resolveSystemMessage(contract, 1)).thenReturn("system-prompt");
        when(aiModelService.getLlmClient(7L)).thenReturn(null);
        when(legalAiOrchestrator.runPlaybookPhase(1L, 5L, true))
                .thenReturn(LegalAuditOrchestrationResult.builder()
                        .deterministicOpinions(List.of())
                        .deterministicCount(0)
                        .build());
        when(legalAiOrchestrator.runLlmAuditPhase(any())).thenReturn(List.of(aiItem));

        var result = auditKernel.runFormal(LegalAuditKernelCommand.builder()
                .contract(contract)
                .paragraphs(List.of(paragraph))
                .auditRound(1)
                .failFast(true)
                .playbookEnabled(true)
                .build());

        assertEquals("system-prompt", result.getSystemPrompt());
        assertEquals(1, result.getItems().size());
        verify(legalAiOrchestrator).runPlaybookPhase(1L, 5L, true);
    }

    @Test
    void runPreview_shouldMarkPreviewSourceType() {
        LegalAiPolicyBO policy = LegalAiPolicyBO.builder()
                .modelId(7L)
                .partyRole(LegalAiPolicyConstants.DEFAULT_PARTY_ROLE)
                .auditLevel(LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL)
                .build();
        LegalContractParagraphDO paragraph = LegalContractParagraphDO.builder()
                .paragraphId("p-1").text("条款").build();
        LegalAuditPreviewParseService.PreviewParseResult parsed =
                new LegalAuditPreviewParseService.PreviewParseResult();
        parsed.setParagraphs(List.of(paragraph));
        parsed.setClauses(List.of());
        parsed.setTotalParagraphCount(1);

        AiModelDO model = AiModelDO.builder().id(7L).maxTokens(4096).build();

        LegalAiAuditOpinionItemBO aiItem = new LegalAiAuditOpinionItemBO();
        aiItem.setTitle("预览风险");
        aiItem.setSourceType(LegalOpinionSourceTypeEnum.AI.getCode());

        when(previewParseService.parse(eq(100L), eq("test.docx"), any(Integer.class))).thenReturn(parsed);
        when(skillPackSnapshotService.resolveSnapshotForCreate(eq(5L), eq(policy)))
                .thenReturn("{\"audit\":{\"skillPackId\":1}}");
        when(legalAiModelResolver.requireChatModel(7L)).thenReturn(model);
        when(auditRoleService.resolveSystemMessage(any(LegalContractDO.class), eq(1))).thenReturn("preview-prompt");
        when(reviewPlanCompiler.compile(5L)).thenReturn(null);
        when(deterministicAuditEngine.run(any(), any(), any())).thenReturn(List.of());
        when(aiModelService.getLlmClient(7L)).thenReturn(null);
        when(legalAiOrchestrator.runLlmAuditPhase(any())).thenReturn(List.of(aiItem));

        var result = auditKernel.runPreview(LegalAuditPreviewCommand.builder()
                .policy(policy)
                .sessionId(10L)
                .fileItemId(200L)
                .contractTypeId(5L)
                .infraFileId(100L)
                .fileName("test.docx")
                .build());

        assertEquals("preview-prompt", result.getSystemPrompt());
        assertTrue(result.getItems().stream()
                .allMatch(item -> LegalOpinionSourceTypeEnum.PREVIEW.getCode().equals(item.getSourceType())));
        verify(skillPackSnapshotService).resolveSnapshotForCreate(5L, policy);
        ArgumentCaptor<LegalContractDO> contractCaptor = ArgumentCaptor.forClass(LegalContractDO.class);
        verify(auditRoleService).resolveSystemMessage(contractCaptor.capture(), eq(1));
        assertNotNull(contractCaptor.getValue().getSkillPackSnapshot());
    }

}
