package com.laby.module.legal.framework.agentscope;

import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.framework.agentscope.config.AgentScopeHarnessBuilderSupport;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelFactory;
import com.laby.module.ai.framework.agentscope.session.AgentScopeSessionFactory;
import com.laby.module.ai.framework.agentscope.session.AgentScopeSessionKeyBuilder;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.framework.agentscope.subagent.LegalAuditSubagentDeclarations;
import com.laby.module.legal.framework.agentscope.skill.LegalSkillPackSkillWriter;
import com.laby.module.legal.framework.agentscope.middleware.LegalAuditCompactionSummaryMiddleware;
import com.laby.module.legal.service.agent.LegalAgentToolProvider;
import com.laby.module.legal.service.contract.LegalAuditCompactionSummaryService;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 法务 AI 审核 Orchestrator（含 Sub-agent 试点）。
 */
@Component
public class LegalAuditOrchestratorAgentScopeConfig {

    private static final String ORCHESTRATOR_SYS_PROMPT = """
            你是法务合同 AI 审核编排 Agent。收到审核任务后，请委派 clause-batch-reviewer 子 Agent \
            分析段落批次并返回批审顺序建议；汇总后输出简洁 Markdown 摘要。
            """;

    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private AgentScopeSessionFactory agentScopeSessionFactory;
    @Resource
    private LegalAgentToolProvider legalAgentToolProvider;
    @Resource
    private LegalSkillPackSkillWriter skillPackSkillWriter;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;
    @Resource
    private LegalAuditCompactionSummaryService auditCompactionSummaryService;

    public HarnessAgent buildOrchestrator(LegalContractDO contract, AiModelDO model, AiApiKeyDO apiKey,
                                           String sessionId) {
        Toolkit toolkit = legalAgentToolProvider.buildToolkit(false);
        Path workspace = Paths.get(agentScopeProperties.getWorkspacePath(), "legal-audit",
                String.valueOf(contract.getId()));
        LegalSkillPackResolvedBO auditSkillPack = skillPackSnapshotService
                .resolveFromContract(contract, LegalSkillPackSceneEnum.AUDIT.getCode())
                .orElse(null);
        if (auditSkillPack != null) {
            skillPackSkillWriter.syncSkillPack(workspace, auditSkillPack, "audit");
        }
        Session session = agentScopeSessionFactory.createSession(workspace);
        SessionKey sessionKey = AgentScopeSessionKeyBuilder.legalContract(
                agentScopeProperties.getSessionKeyPrefix(), contract.getId(), "audit:" + sessionId);
        String modelRef = AgentScopeModelFactory.toModelRef(AgentScopeModelFactory.from(model, apiKey));

        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name("legal-audit-orchestrator")
                .sysPrompt(ORCHESTRATOR_SYS_PROMPT)
                .model(AgentScopeModelFactory.buildChatModel(AgentScopeModelFactory.from(model, apiKey),
                        agentScopeProperties.getModelMaxRetries()))
                .toolkit(toolkit)
                .middlewares(buildMiddlewares())
                .session(session)
                .sessionKey(sessionKey)
                .subagent(LegalAuditSubagentDeclarations.clauseBatchReviewer(modelRef))
                .disableDynamicSkills()
                .workspace(workspace);
        if (auditSkillPack != null) {
            builder.skillsEnabled(true);
        }
        AgentScopeHarnessBuilderSupport.applyCommonOptions(builder, agentScopeProperties);
        return builder.build();
    }

    private List<MiddlewareBase> buildMiddlewares() {
        List<MiddlewareBase> middlewares = new ArrayList<>();
        if (auditCompactionSummaryService.isEnabled()) {
            middlewares.add(new LegalAuditCompactionSummaryMiddleware(auditCompactionSummaryService));
        }
        return middlewares;
    }

}
