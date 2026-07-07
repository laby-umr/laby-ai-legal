package com.laby.module.legal.service.agent;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.service.model.AiApiKeyService;
import com.laby.module.legal.service.ai.LegalAiModelResolver;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.enums.contract.LegalContractChatAnswerModeEnum;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.framework.agentscope.LegalAgentScopeConfig;
import com.laby.module.legal.service.agent.bo.LegalContractAgentPendingConfirmBO;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import com.laby.module.legal.tool.agent.LegalAgentToolRuntimeContext;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

/**
 * 从 Confirm 快照重建 HarnessAgent（跨实例 Resume）。
 */
@Service
public class LegalContractAgentRebuildService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalAiModelResolver legalAiModelResolver;
    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private LegalAgentScopeConfig legalAgentScopeConfig;
    @Resource
    private LegalAgentToolProvider legalAgentToolProvider;
    @Resource
    private LegalContractAgentPromptHelper agentPromptHelper;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;
    @Resource
    private AgentScopeProperties agentScopeProperties;

    public LegalContractAgentRuntimeRegistry.PendingRun rebuild(LegalContractAgentPendingConfirmBO snapshot) {
        LegalContractDO contract = contractMapper.selectById(snapshot.getContractId());
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        AiModelDO model = legalAiModelResolver.resolveModelOrDefault(contract.getModelId());
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        LegalContractChatAnswerModeEnum answerMode = LegalContractChatAnswerModeEnum.of(snapshot.getAnswerMode());
        List<String> skillPackTools = skillPackSnapshotService
                .resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                .map(LegalSkillPackResolvedBO::getToolNames)
                .orElse(List.of());
        LegalSkillPackResolvedBO skillPack = skillPackSnapshotService
                .resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                .orElse(null);
        String sysPrompt = agentPromptHelper.buildSystemPrompt(contract, answerMode, snapshot.isAllowProposal());
        RuntimeContext runtimeContext = legalAgentToolProvider.buildAgentRuntimeContext(
                contract, snapshot.getSessionId(), snapshot.isAllowProposal(),
                agentScopeProperties.isCompactionSummaryPersist());
        LegalAgentToolRuntimeContext toolContext = runtimeContext.get(LegalAgentToolRuntimeContext.class);
        HarnessAgent agent = legalAgentScopeConfig.buildAgent(contract, model, apiKey, snapshot.isAllowProposal(),
                skillPackTools, sysPrompt, snapshot.getSessionId(), skillPack, toolContext);
        RequireUserConfirmEvent confirmEvent = new RequireUserConfirmEvent(
                snapshot.getConfirmId(), rebuildToolCalls(snapshot.getToolCalls()));

        LegalContractAgentRuntimeRegistry.PendingRun pending = new LegalContractAgentRuntimeRegistry.PendingRun();
        pending.setSessionId(snapshot.getSessionId());
        pending.setContractId(snapshot.getContractId());
        pending.setAllowProposal(snapshot.isAllowProposal());
        pending.setAnswerMode(snapshot.getAnswerMode());
        pending.setAgent(agent);
        pending.setRuntimeContext(runtimeContext);
        pending.setConfirmEvent(confirmEvent);
        pending.setAwaitingConfirm(true);
        return pending;
    }

    private static List<ToolUseBlock> rebuildToolCalls(List<LegalContractAgentPendingConfirmBO.ToolCallSnapshot> snapshots) {
        List<ToolUseBlock> toolCalls = new ArrayList<>();
        if (CollUtil.isEmpty(snapshots)) {
            return toolCalls;
        }
        for (LegalContractAgentPendingConfirmBO.ToolCallSnapshot snapshot : snapshots) {
            ToolUseBlock.Builder builder = ToolUseBlock.builder()
                    .id(snapshot.getId())
                    .name(snapshot.getName());
            if (snapshot.getInput() != null) {
                builder.input(snapshot.getInput());
            }
            toolCalls.add(builder.build());
        }
        return toolCalls;
    }
}
