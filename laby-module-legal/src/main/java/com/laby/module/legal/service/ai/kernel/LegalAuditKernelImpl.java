package com.laby.module.legal.service.ai.kernel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.service.ai.LegalAiModelResolver;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.enums.ai.LegalAiAuditKernelConstants;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelCommand;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelResult;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditPreviewCommand;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.contract.LegalContractAuditRoleService;
import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import com.laby.module.legal.service.orchestrator.LegalAiOrchestrator;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditPipelineCommand;
import com.laby.module.legal.service.orchestrator.bo.LegalAuditOrchestrationResult;
import com.laby.module.legal.service.playbook.LegalDeterministicAuditEngine;
import com.laby.module.legal.service.playbook.LegalReviewPlanCompiler;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import com.laby.module.legal.service.skillpack.LegalSkillPackRegistry;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackModelPolicyBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 法务 AI 审核内核实现
 */
@Slf4j
@Service
public class LegalAuditKernelImpl implements LegalAuditKernel {

    @Resource
    private AiModelService aiModelService;
    @Resource
    private LegalAiModelResolver legalAiModelResolver;
    @Resource
    private LegalContractAuditRoleService auditRoleService;
    @Resource
    private LegalAiOrchestrator legalAiOrchestrator;
    @Resource
    private LegalReviewPlanCompiler reviewPlanCompiler;
    @Resource
    private LegalDeterministicAuditEngine deterministicAuditEngine;
    @Resource
    private LegalAuditPreviewParseService previewParseService;
    @Resource
    private LegalSkillPackRegistry skillPackRegistry;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;

    @Override
    public LegalAuditKernelResult runFormal(LegalAuditKernelCommand command) {
        LegalContractDO contract = command.getContract();
        List<LegalContractParagraphDO> paragraphs = command.getParagraphs();
        int auditRound = command.getAuditRound();

        AiModelDO model = legalAiModelResolver.requireChatModel(contract.getModelId());
        String systemPrompt = auditRoleService.resolveSystemMessage(contract, auditRound);
        int auditMaxTokens = resolveAuditMaxTokens(model.getMaxTokens(), contract);

        LegalAuditOrchestrationResult playbookResult = command.isPlaybookEnabled()
                ? legalAiOrchestrator.runPlaybookPhase(contract.getId(), contract.getContractTypeId(), true)
                : LegalAuditOrchestrationResult.builder()
                .deterministicOpinions(List.of())
                .deterministicCount(0)
                .build();

        List<LegalAiAuditOpinionItemBO> aiItems = legalAiOrchestrator.runLlmAuditPhase(
                LegalAiAuditPipelineCommand.builder()
                        .llmClient(aiModelService.getLlmClient(model.getId()))
                        .systemPrompt(systemPrompt)
                        .contract(contract)
                        .paragraphs(paragraphs)
                        .auditRound(auditRound)
                        .failFast(command.isFailFast())
                        .maxTokens(auditMaxTokens)
                        .build());

        List<LegalAiAuditOpinionItemBO> items = mergeAuditOpinions(playbookResult.getDeterministicOpinions(), aiItems);
        return LegalAuditKernelResult.builder()
                .items(items)
                .deterministicCount(playbookResult.getDeterministicCount())
                .paragraphCount(paragraphs.size())
                .systemPrompt(systemPrompt)
                .modelId(model.getId())
                .build();
    }

    @Override
    public LegalAuditKernelResult runPreview(LegalAuditPreviewCommand command) {
        LegalAiPolicyBO policy = command.getPolicy();
        int maxParagraphs = command.getMaxParagraphs() != null
                ? command.getMaxParagraphs()
                : LegalAiAuditKernelConstants.MAX_PREVIEW_PARAGRAPHS;

        LegalAuditPreviewParseService.PreviewParseResult parsed = previewParseService.parse(
                command.getInfraFileId(), command.getFileName(), maxParagraphs);
        List<LegalContractParagraphDO> paragraphs = parsed.getParagraphs();
        if (CollUtil.isEmpty(paragraphs)) {
            return LegalAuditKernelResult.builder()
                    .items(List.of())
                    .deterministicCount(0)
                    .paragraphCount(0)
                    .modelId(policy.getModelId())
                    .build();
        }

        LegalContractDO contract = buildPreviewContract(command, policy);
        AiModelDO model = legalAiModelResolver.requireChatModel(policy.getModelId());
        String systemPrompt = auditRoleService.resolveSystemMessage(contract, 1);
        int auditMaxTokens = resolveAuditMaxTokens(model.getMaxTokens(), contract);

        List<LegalAuditOpinionDraftBO> deterministic = List.of();
        int deterministicCount = 0;
        if (command.getContractTypeId() != null) {
            LegalReviewPlanBO plan = reviewPlanCompiler.compile(command.getContractTypeId());
            deterministic = deterministicAuditEngine.run(plan, parsed.getClauses(), paragraphs);
            deterministicCount = deterministic.size();
        }

        List<LegalAiAuditOpinionItemBO> aiItems = legalAiOrchestrator.runLlmAuditPhase(
                LegalAiAuditPipelineCommand.builder()
                        .llmClient(aiModelService.getLlmClient(model.getId()))
                        .systemPrompt(systemPrompt)
                        .contract(contract)
                        .paragraphs(paragraphs)
                        .auditRound(1)
                        .failFast(true)
                        .maxTokens(auditMaxTokens)
                        .build());

        List<LegalAiAuditOpinionItemBO> items = mergeAuditOpinions(deterministic, aiItems);
        markPreviewSource(items);

        if (parsed.getTotalParagraphCount() > paragraphs.size()) {
            log.info("[runPreview][sessionId={} fileItemId={}] 预览截断段落 {}/{}",
                    command.getSessionId(), command.getFileItemId(),
                    paragraphs.size(), parsed.getTotalParagraphCount());
        }

        return LegalAuditKernelResult.builder()
                .items(items)
                .deterministicCount(deterministicCount)
                .paragraphCount(paragraphs.size())
                .systemPrompt(systemPrompt)
                .modelId(model.getId())
                .build();
    }

    private LegalContractDO buildPreviewContract(LegalAuditPreviewCommand command, LegalAiPolicyBO policy) {
        Long syntheticId = command.getSessionId() != null ? -Math.abs(command.getSessionId()) : -1L;
        String snapshotJson = skillPackSnapshotService.resolveSnapshotForCreate(command.getContractTypeId(), policy);
        return LegalContractDO.builder()
                .id(syntheticId)
                .title(StrUtil.blankToDefault(command.getFileName(), "预览合同"))
                .contractTypeId(command.getContractTypeId())
                .partyRole(policy.getPartyRole())
                .auditLevel(policy.getAuditLevel())
                .modelId(policy.getModelId())
                .auditRoleId(policy.getAuditRoleId())
                .reauditRoleId(policy.getReauditRoleId())
                .skillPackSnapshot(snapshotJson)
                .build();
    }

    private static void markPreviewSource(List<LegalAiAuditOpinionItemBO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (LegalAiAuditOpinionItemBO item : items) {
            if (StrUtil.isBlank(item.getSourceType())
                    || LegalOpinionSourceTypeEnum.AI.getCode().equalsIgnoreCase(item.getSourceType())) {
                item.setSourceType(LegalOpinionSourceTypeEnum.PREVIEW.getCode());
            }
        }
    }
    private int resolveAuditMaxTokens(Integer configured, LegalContractDO contract) {
        int tokens = configured == null ? LegalAiAuditKernelConstants.MIN_AUDIT_MAX_TOKENS : configured;
        Integer policyMax = null;
        if (contract.getId() != null) {
            policyMax = skillPackSnapshotService
                    .resolveFromContract(contract, LegalSkillPackSceneEnum.AUDIT.getCode())
                    .flatMap(skillPackRegistry::parseModelPolicy)
                    .map(LegalSkillPackModelPolicyBO::getMaxTokens)
                    .filter(max -> max != null && max > 0)
                    .orElse(null);
        } else if (contract.getContractTypeId() != null) {
            policyMax = skillPackRegistry
                    .resolveForContractType(contract.getContractTypeId(), LegalSkillPackSceneEnum.AUDIT.getCode())
                    .flatMap(skillPackRegistry::parseModelPolicy)
                    .map(LegalSkillPackModelPolicyBO::getMaxTokens)
                    .filter(max -> max != null && max > 0)
                    .orElse(null);
        }
        if (policyMax != null && policyMax >= LegalAiAuditKernelConstants.MIN_AUDIT_MAX_TOKENS) {
            tokens = Math.min(policyMax, LegalAiAuditKernelConstants.MAX_AUDIT_MAX_TOKENS);
        }
        if (tokens < LegalAiAuditKernelConstants.MIN_AUDIT_MAX_TOKENS) {
            tokens = LegalAiAuditKernelConstants.MIN_AUDIT_MAX_TOKENS;
        }
        return Math.min(tokens, LegalAiAuditKernelConstants.MAX_AUDIT_MAX_TOKENS);
    }

    private static List<LegalAiAuditOpinionItemBO> mergeAuditOpinions(List<LegalAuditOpinionDraftBO> deterministic,
                                                                      List<LegalAiAuditOpinionItemBO> aiItems) {
        List<LegalAiAuditOpinionItemBO> merged = new ArrayList<>();
        if (CollUtil.isNotEmpty(deterministic)) {
            for (LegalAuditOpinionDraftBO draft : deterministic) {
                merged.add(toOpinionItem(draft));
            }
        }
        if (CollUtil.isNotEmpty(aiItems)) {
            merged.addAll(aiItems);
        }
        return merged;
    }

    private static LegalAiAuditOpinionItemBO toOpinionItem(LegalAuditOpinionDraftBO draft) {
        LegalAiAuditOpinionItemBO item = new LegalAiAuditOpinionItemBO();
        item.setClauseType(draft.getClauseType());
        item.setRiskLevel(draft.getRiskLevel());
        item.setTitle(draft.getTitle());
        item.setContent(draft.getContent());
        item.setSuggestion(draft.getSuggestion());
        item.setParagraphId(draft.getParagraphId());
        item.setClauseId(draft.getClauseId());
        item.setReferenceClause(draft.getReferenceClause());
        item.setSourceType(StrUtil.blankToDefault(draft.getSourceType(), LegalOpinionSourceTypeEnum.RULE.getCode()));
        item.setSourceId(draft.getSourceId());
        item.setChangeType(draft.getChangeType());
        item.setOldText(draft.getOldText());
        item.setNewText(draft.getNewText());
        item.setEvidenceRefs(draft.getEvidenceRefs());
        return item;
    }

}
