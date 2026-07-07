package com.laby.module.legal.service.orchestrator;

import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditPipelineCommand;
import com.laby.module.legal.service.orchestrator.bo.LegalAuditOrchestrationResult;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;

import java.util.List;

/**
 * 法务 AI 统一编排（Wave2 骨架）
 */
public interface LegalAiOrchestrator {

    /**
     * 运行 Playbook 确定性检查
     */
    List<LegalAuditOpinionDraftBO> runDeterministicAudit(Long contractId, LegalReviewPlanBO plan);

    /**
     * Playbook 阶段编排（编译规则 + 确定性检查）
     */
    LegalAuditOrchestrationResult runPlaybookPhase(Long contractId, Long contractTypeId, boolean playbookEnabled);

    /**
     * LLM 分批审核阶段
     */
    List<LegalAiAuditOpinionItemBO> runLlmAuditPhase(LegalAiAuditPipelineCommand command);

}
