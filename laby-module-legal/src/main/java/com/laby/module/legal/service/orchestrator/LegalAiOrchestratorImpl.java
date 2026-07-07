package com.laby.module.legal.service.orchestrator;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiModelTypeEnum;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.service.model.AiApiKeyService;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.mysql.clause.LegalContractClauseMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.framework.agentscope.LegalAuditOrchestratorAgentScopeConfig;
import com.laby.module.legal.service.agent.LegalAgentToolProvider;
import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditPipelineCommand;
import com.laby.module.legal.service.orchestrator.bo.LegalAuditOrchestrationResult;
import com.laby.module.legal.service.playbook.LegalDeterministicAuditEngine;
import com.laby.module.legal.service.playbook.LegalReviewPlanCompiler;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LegalAiOrchestratorImpl implements LegalAiOrchestrator {

    private static final int SUBAGENT_PILOT_TIMEOUT_SECONDS = 120;
    private static final int SUBAGENT_PILOT_PARAGRAPH_PREVIEW = 8;

    @Resource
    private LegalDeterministicAuditEngine deterministicAuditEngine;
    @Resource
    private LegalContractClauseMapper clauseMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalReviewPlanCompiler reviewPlanCompiler;
    @Resource
    private LegalAiAuditPipelineService auditPipelineService;
    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private LegalAuditOrchestratorAgentScopeConfig auditOrchestratorAgentScopeConfig;
    @Resource
    private AiModelService aiModelService;
    @Resource
    private AiApiKeyService aiApiKeyService;
    @Resource
    private LegalAgentToolProvider legalAgentToolProvider;

    @Override
    public List<LegalAuditOpinionDraftBO> runDeterministicAudit(Long contractId, LegalReviewPlanBO plan) {
        List<LegalContractClauseDO> clauses = clauseMapper.selectListByContractId(contractId);
        List<LegalContractParagraphDO> paragraphs = paragraphMapper.selectListByContractId(contractId);
        return deterministicAuditEngine.run(plan, clauses, paragraphs);
    }

    @Override
    public LegalAuditOrchestrationResult runPlaybookPhase(Long contractId, Long contractTypeId,
                                                          boolean playbookEnabled) {
        if (!playbookEnabled) {
            return LegalAuditOrchestrationResult.builder()
                    .deterministicOpinions(Collections.emptyList())
                    .deterministicCount(0)
                    .build();
        }
        LegalReviewPlanBO plan = reviewPlanCompiler.compile(contractTypeId);
        List<LegalAuditOpinionDraftBO> opinions = runDeterministicAudit(contractId, plan);
        return LegalAuditOrchestrationResult.builder()
                .deterministicOpinions(opinions)
                .deterministicCount(opinions.size())
                .build();
    }

    @Override
    public List<LegalAiAuditOpinionItemBO> runLlmAuditPhase(LegalAiAuditPipelineCommand command) {
        if (agentScopeProperties.isAuditSubagentPilot()) {
            runSubagentPilotPlanning(command);
        }
        return auditPipelineService.runLlmAuditBatches(command);
    }

    private void runSubagentPilotPlanning(LegalAiAuditPipelineCommand command) {
        LegalContractDO contract = command.getContract();
        if (contract == null || contract.getId() == null) {
            return;
        }
        try {
            AiModelDO model = resolveAuditModel(contract);
            AiApiKeyDO apiKey = aiApiKeyService.validateApiKey(model.getKeyId());
            String sessionId = "pilot-" + contract.getId() + "-r" + command.getAuditRound();
            RuntimeContext runtimeContext = legalAgentToolProvider.buildAgentRuntimeContext(
                    contract, sessionId, false, agentScopeProperties.isCompactionSummaryPersist());
            HarnessAgent agent = auditOrchestratorAgentScopeConfig.buildOrchestrator(
                    contract, model, apiKey, sessionId);
            UserMessage message = UserMessage.builder()
                    .textContent(buildSubagentPilotPrompt(command))
                    .build();
            Msg result = agent.call(List.of(message), runtimeContext)
                    .timeout(Duration.ofSeconds(SUBAGENT_PILOT_TIMEOUT_SECONDS))
                    .block();
            log.info("[runSubagentPilotPlanning][contractId={} round={}] {}",
                    contract.getId(), command.getAuditRound(),
                    result != null ? StrUtil.sub(result.getTextContent(), 0, 500) : "(empty)");
        } catch (Exception ex) {
            log.warn("[runSubagentPilotPlanning][contractId={}] 试点跳过: {}",
                    contract.getId(), ex.getMessage());
        }
    }

    private static String buildSubagentPilotPrompt(LegalAiAuditPipelineCommand command) {
        int total = command.getParagraphs() != null ? command.getParagraphs().size() : 0;
        String preview = command.getParagraphs() == null ? "" : command.getParagraphs().stream()
                .limit(SUBAGENT_PILOT_PARAGRAPH_PREVIEW)
                .map(p -> StrUtil.blankToDefault(p.getParagraphId(), "?") + ": "
                        + StrUtil.sub(StrUtil.blankToDefault(p.getText(), ""), 0, 80))
                .collect(Collectors.joining("\n"));
        return "合同第 " + command.getAuditRound() + " 轮审核，共 " + total
                + " 段待审。请委派 clause-batch-reviewer 子 Agent 给出批审顺序与风险优先级建议。\n\n段落预览：\n"
                + preview;
    }

    private AiModelDO resolveAuditModel(LegalContractDO contract) {
        if (contract.getModelId() != null) {
            return aiModelService.validateModel(contract.getModelId());
        }
        return aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
    }

}
