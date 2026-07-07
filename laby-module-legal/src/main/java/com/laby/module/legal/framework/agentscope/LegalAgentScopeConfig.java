package com.laby.module.legal.framework.agentscope;

import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.framework.agentscope.config.AgentScopeHarnessBuilderSupport;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelFactory;
import com.laby.module.ai.framework.agentscope.session.AgentScopeSessionFactory;
import com.laby.module.ai.framework.agentscope.session.AgentScopeSessionKeyBuilder;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.framework.agentscope.middleware.LegalAgentSseMiddleware;
import com.laby.module.legal.framework.agentscope.middleware.LegalAgentToolContextMiddleware;
import com.laby.module.legal.framework.agentscope.middleware.LegalAgentTraceMiddleware;
import com.laby.module.legal.framework.agentscope.middleware.LegalContractCompactionSummaryMiddleware;
import com.laby.module.legal.framework.agentscope.middleware.LegalTenantMiddleware;
import com.laby.module.legal.framework.agentscope.permission.LegalAgentPermissionContextFactory;
import com.laby.module.legal.framework.agentscope.skill.LegalSkillPackSkillWriter;
import com.laby.module.legal.service.agent.LegalAgentStepLogService;
import com.laby.module.legal.service.agent.LegalAgentToolProvider;
import com.laby.module.legal.service.contract.LegalContractCompactionSummaryService;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import com.laby.module.legal.tool.agent.LegalAgentToolRuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 法务合同 Agent HarnessAgent 构建器。
 */
@Component
public class LegalAgentScopeConfig {

    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private AgentScopeSessionFactory agentScopeSessionFactory;
    @Resource
    private LegalAgentToolProvider legalAgentToolProvider;
    @Resource
    private LegalAgentStepLogService agentStepLogService;
    @Resource
    private LegalSkillPackSkillWriter skillPackSkillWriter;
    @Resource
    private LegalContractCompactionSummaryService compactionSummaryService;

    public HarnessAgent buildAgent(LegalContractDO contract, AiModelDO model, AiApiKeyDO apiKey,
                                   boolean allowProposal, List<String> skillPackTools, String sysPrompt,
                                   String sessionId, LegalSkillPackResolvedBO skillPack,
                                   LegalAgentToolRuntimeContext toolContext) {
        Toolkit toolkit = legalAgentToolProvider.buildToolkit(allowProposal, skillPackTools);
        ToolExecutionContext toolExecutionContext = toolContext != null
                ? ToolExecutionContext.builder().register(toolContext).build()
                : ToolExecutionContext.builder()
                .register(LegalAgentToolRuntimeContext.from(
                        legalAgentToolProvider.buildToolContext(contract.getId(), sessionId, allowProposal)))
                .build();

        Path workspace = Paths.get(agentScopeProperties.getWorkspacePath(), "legal",
                String.valueOf(contract.getId()));
        if (skillPack != null) {
            skillPackSkillWriter.syncSkillPack(workspace, skillPack, "chat");
        }

        Session session = agentScopeSessionFactory.createSession(workspace);
        SessionKey sessionKey = AgentScopeSessionKeyBuilder.legalContract(
                agentScopeProperties.getSessionKeyPrefix(), contract.getId(), sessionId);

        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name("legal-contract-agent")
                .sysPrompt(sysPrompt)
                .model(AgentScopeModelFactory.buildChatModel(AgentScopeModelFactory.from(model, apiKey),
                        agentScopeProperties.getModelMaxRetries()))
                .toolkit(toolkit)
                .toolExecutionContext(toolExecutionContext)
                .middlewares(buildMiddlewares())
                .permissionContext(LegalAgentPermissionContextFactory.build(allowProposal))
                .enablePendingToolRecovery(true)
                .session(session)
                .sessionKey(sessionKey)
                .disableSubagents()
                .disableFilesystemTools()
                .disableShellTool()
                .workspace(workspace);
        if (skillPack == null) {
            builder.disableDynamicSkills();
        } else {
            builder.skillsEnabled(true);
        }
        AgentScopeHarnessBuilderSupport.applyCommonOptions(builder, agentScopeProperties);
        return builder.build();
    }

    private List<MiddlewareBase> buildMiddlewares() {
        List<MiddlewareBase> middlewares = new ArrayList<>();
        middlewares.add(new LegalAgentToolContextMiddleware());
        middlewares.add(new LegalTenantMiddleware());
        middlewares.add(new LegalAgentTraceMiddleware(agentStepLogService));
        middlewares.add(new LegalAgentSseMiddleware());
        if (compactionSummaryService.isEnabled()) {
            middlewares.add(new LegalContractCompactionSummaryMiddleware(compactionSummaryService));
        }
        return middlewares;
    }

}
