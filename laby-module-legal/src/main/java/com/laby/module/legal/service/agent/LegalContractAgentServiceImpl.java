package com.laby.module.legal.service.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.service.model.AiApiKeyService;
import com.laby.module.legal.service.ai.LegalAiModelResolver;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.enums.contract.LegalContractChatAnswerModeEnum;
import com.laby.module.legal.framework.agentscope.LegalAgentScopeConfig;
import com.laby.module.legal.framework.agentscope.permission.LegalAgentPermissionContextFactory;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.tool.agent.LegalAgentSseEventHolder;
import com.laby.module.legal.tool.agent.LegalAgentToolRuntimeContext;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static com.laby.framework.common.pojo.CommonResult.success;

/**
 * 法务合同 Agent 编排 Service 实现
 */
@Slf4j
@Service
public class LegalContractAgentServiceImpl implements LegalContractAgentService {

    private static final Duration HEARTBEAT_INTERVAL = Duration.ofMillis(500);

    @Resource
    private LegalAiModelResolver legalAiModelResolver;
    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private LegalAgentToolProvider legalAgentToolProvider;
    @Resource
    private LegalAgentStepLogService agentStepLogService;
    @Resource
    private LegalAgentProposalService proposalService;
    @Resource
    private LegalContractAgentPromptHelper agentPromptHelper;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;
    @Resource
    private LegalAgentScopeConfig legalAgentScopeConfig;
    @Resource
    private LegalAgentSessionGuard agentSessionGuard;
    @Resource
    private LegalContractAgentRuntimeRegistry runtimeRegistry;
    @Resource
    private AgentScopeProperties agentScopeProperties;

    @Override
    public Flux<CommonResult<LegalContractChatRespVO>> runStream(LegalContractDO contract,
                                                                 LegalContractChatReqVO reqVO) {
        initAgentStepLog(reqVO, contract);
        String sessionId = reqVO.getSessionId();
        if (runtimeRegistry.isAwaitingConfirm(sessionId)) {
            return Flux.just(agentErrorEvent(sessionId, "请先处理待确认的 Agent 操作"));
        }
        if (!agentSessionGuard.tryAcquire(sessionId)) {
            return Flux.just(agentErrorEvent(sessionId, "Agent 正在处理上一条消息，请稍后再试"));
        }
        LegalAgentSseEventHolder.bindSession(sessionId);

        AiModelDO model = legalAiModelResolver.resolveModelOrDefault(contract.getModelId());
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        LegalContractChatAnswerModeEnum answerMode = LegalContractChatAnswerModeEnum.of(reqVO.getAnswerMode());
        boolean allowProposal = resolveAllowProposal(reqVO);
        List<String> skillPackTools = skillPackSnapshotService
                .resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                .map(LegalSkillPackResolvedBO::getToolNames)
                .orElse(List.of());
        LegalSkillPackResolvedBO skillPack = skillPackSnapshotService
                .resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                .orElse(null);
        String sysPrompt = agentPromptHelper.buildSystemPrompt(contract, answerMode, allowProposal);
        RuntimeContext runtimeContext = legalAgentToolProvider.buildAgentRuntimeContext(
                contract, sessionId, allowProposal, agentScopeProperties.isCompactionSummaryPersist());
        LegalAgentToolRuntimeContext toolContext = runtimeContext.get(LegalAgentToolRuntimeContext.class);
        HarnessAgent agent = legalAgentScopeConfig.buildAgent(contract, model, apiKey, allowProposal,
                skillPackTools, sysPrompt, sessionId, skillPack, toolContext);
        runtimeRegistry.register(sessionId, agent, runtimeContext, contract.getId(), allowProposal,
                answerMode.getMode());
        List<Msg> messages = buildAgentMessages(reqVO);

        Flux<CommonResult<LegalContractChatRespVO>> start = Flux.just(success(new LegalContractChatRespVO()
                .setSessionId(sessionId)));

        Flux<CommonResult<LegalContractChatRespVO>> llmContent = agent.streamEvents(messages, runtimeContext)
                .flatMap(event -> {
                    CommonResult<LegalContractChatRespVO> mapped =
                            mapAgentEventToSse(event, sessionId, runtimeRegistry);
                    return mapped != null ? Flux.just(mapped) : Flux.empty();
                })
                .onErrorResume(ex -> {
                    log.warn("[runStream][sessionId={}] Agent 流式异常", sessionId, ex);
                    return Flux.just(agentErrorEvent(sessionId, resolveStreamErrorMessage(ex)));
                })
                // merge + takeUntilOther 会各订阅一次，必须 share 避免同一 Agent 并发 streamEvents
                .share();

        Mono<Void> llmTerminal = llmContent.ignoreElements().then();

        Flux<CommonResult<LegalContractChatRespVO>> sideEvents = Flux.interval(HEARTBEAT_INTERVAL)
                .takeUntilOther(llmTerminal)
                .concatMap(tick -> drainSseEvents(sessionId, true))
                .onErrorResume(ex -> Flux.empty());

        Flux<CommonResult<LegalContractChatRespVO>> tailDrain = Flux.defer(() -> drainSseEvents(sessionId, false));
        Flux<CommonResult<LegalContractChatRespVO>> confirmResumeDrain = Flux.defer(() ->
                runtimeRegistry.isAwaitingConfirm(sessionId)
                        ? drainWhileSessionActive(sessionId)
                        : Flux.empty());

        Flux<CommonResult<LegalContractChatRespVO>> agentFlux = Flux.merge(sideEvents, llmContent)
                .concatWith(tailDrain)
                .timeout(Duration.ofSeconds(AGENT_TIMEOUT_SECONDS))
                .onErrorResume(ex -> {
                    log.warn("[runStream][sessionId={}] Agent 主流异常", sessionId, ex);
                    return Flux.just(agentErrorEvent(sessionId, resolveStreamErrorMessage(ex)));
                })
                .doOnComplete(() -> cleanupRuntimeIfIdle(sessionId));

        return Flux.concat(start, agentFlux, confirmResumeDrain)
                .doFinally(signal -> cleanupRuntimeIfIdle(sessionId));
    }

    private void cleanupRuntimeIfIdle(String sessionId) {
        if (!runtimeRegistry.isAwaitingConfirm(sessionId)) {
            agentSessionGuard.release(sessionId);
            runtimeRegistry.remove(sessionId);
            LegalAgentToolProvider.removeSessionToolContext(sessionId);
        }
    }

    @Override
    public LegalContractChatRespVO runSync(LegalContractDO contract,
                                           LegalContractChatReqVO reqVO) {
        initAgentStepLog(reqVO, contract);
        String sessionId = reqVO.getSessionId();
        if (!agentSessionGuard.tryAcquire(sessionId)) {
            throw new IllegalStateException("Agent 正在处理上一条消息，请稍后再试");
        }
        try {
            AiModelDO model = legalAiModelResolver.resolveModelOrDefault(contract.getModelId());
            AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
            LegalContractChatAnswerModeEnum answerMode = LegalContractChatAnswerModeEnum.of(reqVO.getAnswerMode());
            boolean allowProposal = resolveAllowProposal(reqVO);
            List<String> skillPackTools = skillPackSnapshotService
                    .resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                    .map(LegalSkillPackResolvedBO::getToolNames)
                    .orElse(List.of());
            LegalSkillPackResolvedBO skillPack = skillPackSnapshotService
                    .resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                    .orElse(null);
            String sysPrompt = agentPromptHelper.buildSystemPrompt(contract, answerMode, allowProposal);
            RuntimeContext runtimeContext = legalAgentToolProvider.buildAgentRuntimeContext(
                    contract, sessionId, allowProposal, agentScopeProperties.isCompactionSummaryPersist());
            LegalAgentToolRuntimeContext toolContext = runtimeContext.get(LegalAgentToolRuntimeContext.class);
            HarnessAgent agent = legalAgentScopeConfig.buildAgent(contract, model, apiKey, allowProposal,
                    skillPackTools, sysPrompt, sessionId, skillPack, toolContext);
            runtimeRegistry.register(sessionId, agent, runtimeContext, contract.getId(), allowProposal,
                answerMode.getMode());
            List<Msg> messages = buildAgentMessages(reqVO);

            Msg result = agent.call(messages, runtimeContext)
                    .timeout(Duration.ofSeconds(AGENT_TIMEOUT_SECONDS))
                    .onErrorMap(TimeoutException.class,
                            ex -> new IllegalStateException("Agent 问答超时，请稍后重试"))
                    .block();
            String content = result != null ? StrUtil.nullToDefault(result.getTextContent(), "") : "";
            agentStepLogService.logLlm("Agent 同步问答完成: " + StrUtil.sub(content, 0, 100));
            return new LegalContractChatRespVO()
                    .setContent(content)
                    .setSessionId(sessionId);
        } catch (RuntimeException ex) {
            log.warn("[runSync][sessionId={}] Agent 同步异常", sessionId, ex);
            throw ex;
        } finally {
            if (!runtimeRegistry.isAwaitingConfirm(sessionId)) {
                agentSessionGuard.release(sessionId);
                runtimeRegistry.remove(sessionId);
                LegalAgentToolProvider.removeSessionToolContext(sessionId);
            }
            LegalAgentStepLogContext.removeSession(sessionId);
        }
    }

    @Override
    public void executeProposal(String proposalNo) {
        proposalService.executeProposal(proposalNo);
    }

    @Override
    public void cancelProposal(String proposalNo) {
        proposalService.cancelProposal(proposalNo);
    }

    private List<Msg> buildAgentMessages(LegalContractChatReqVO reqVO) {
        return List.of(new UserMessage(reqVO.getMessage().trim()));
    }

    private static CommonResult<LegalContractChatRespVO> mapAgentEventToSse(AgentEvent event, String sessionId,
                                                                            LegalContractAgentRuntimeRegistry registry) {
        return mapAgentEventToSsePublic(event, sessionId, registry);
    }

    public static CommonResult<LegalContractChatRespVO> mapAgentEventToSsePublic(AgentEvent event, String sessionId,
                                                                                 LegalContractAgentRuntimeRegistry registry) {
        if (event.getType() == AgentEventType.REQUIRE_USER_CONFIRM && event instanceof RequireUserConfirmEvent confirmEvent) {
            registry.saveConfirmEvent(sessionId, confirmEvent);
            String toolName = CollUtil.isNotEmpty(confirmEvent.getToolCalls())
                    ? StrUtil.blankToDefault(confirmEvent.getToolCalls().get(0).getName(), "tool") : "tool";
            String summary = buildConfirmSummary(toolName);
            LegalAgentSseEventHolder.pushConfirmRequired(sessionId, confirmEvent.getReplyId(), toolName, summary);
            return success(new LegalContractChatRespVO()
                    .setEventType("confirm_required")
                    .setConfirmId(confirmEvent.getReplyId())
                    .setToolName(toolName)
                    .setConfirmSummary(summary)
                    .setSessionId(sessionId));
        }
        if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
            return success(new LegalContractChatRespVO()
                    .setContent(StrUtil.nullToDefault(((TextBlockDeltaEvent) event).getDelta(), ""))
                    .setSessionId(sessionId));
        }
        if (event.getType() == AgentEventType.THINKING_BLOCK_DELTA) {
            return success(new LegalContractChatRespVO()
                    .setReasoningContent(StrUtil.nullToDefault(((ThinkingBlockDeltaEvent) event).getDelta(), ""))
                    .setSessionId(sessionId));
        }
        return null;
    }

    private Flux<CommonResult<LegalContractChatRespVO>> drainSseEvents(String sessionId, boolean allowHeartbeat) {
        return Flux.fromIterable(drainSseEventsPublic(sessionId, allowHeartbeat));
    }

    private Flux<CommonResult<LegalContractChatRespVO>> drainWhileSessionActive(String sessionId) {
        return Flux.interval(HEARTBEAT_INTERVAL)
                .concatMap(tick -> drainSseEvents(sessionId, true))
                .takeWhile(tick -> runtimeRegistry.get(sessionId) != null)
                .take(Duration.ofSeconds(AGENT_TIMEOUT_SECONDS));
    }

    public static List<CommonResult<LegalContractChatRespVO>> drainSseEventsPublic(String sessionId, boolean allowHeartbeat) {
        List<LegalAgentSseEventHolder.SseEvent> events = LegalAgentSseEventHolder.pollAll(sessionId);
        if (CollUtil.isNotEmpty(events)) {
            return events.stream()
                    .map(event -> success(toAgentEventResp(event, sessionId)))
                    .toList();
        }
        if (!allowHeartbeat) {
            return List.of();
        }
        return List.of(success(new LegalContractChatRespVO().setSessionId(sessionId)));
    }

    private static String resolveStreamErrorMessage(Throwable ex) {
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return "Agent 问答超时，请稍后重试";
        }
        String message = StrUtil.blankToDefault(ex.getMessage(), "未知错误");
        if (message.contains("Agent is still running")) {
            return "Agent 正在处理上一条消息，请稍后再试";
        }
        return "Agent 问答中断，请稍后重试";
    }

    private void initAgentStepLog(LegalContractChatReqVO reqVO, LegalContractDO contract) {
        if (StrUtil.isBlank(reqVO.getSessionId())) {
            reqVO.setSessionId(IdUtil.fastSimpleUUID());
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null && contract.getTenantId() != null) {
            tenantId = contract.getTenantId();
        }
        LegalAgentStepLogContext.init(contract.getId(), reqVO.getSessionId(), userId,
                resolveAllowProposal(reqVO), tenantId, SecurityFrameworkUtils.getLoginUser());
        agentStepLogService.logLlm("Agent 问答开始: " + StrUtil.sub(StrUtil.trim(reqVO.getMessage()), 0, 100));
    }

    private static boolean resolveAllowProposal(LegalContractChatReqVO reqVO) {
        return Boolean.TRUE.equals(reqVO.getAllowProposal());
    }

    private static String buildConfirmSummary(String toolName) {
        if (LegalAgentPermissionContextFactory.TOOL_PROPOSE_ADOPT.equals(toolName)) {
            return "Agent 将生成「采纳意见」操作提案，仍需您在提案卡片上确认后才会写入合同";
        }
        if (LegalAgentPermissionContextFactory.TOOL_PROPOSE_SKIP.equals(toolName)) {
            return "Agent 将生成「跳过段落审核」操作提案，仍需您在提案卡片上确认后才会生效";
        }
        return "Agent 请求调用工具: " + toolName;
    }

    private static CommonResult<LegalContractChatRespVO> agentErrorEvent(String sessionId, String message) {
        return success(new LegalContractChatRespVO()
                .setEventType("error")
                .setContent(message)
                .setSessionId(sessionId));
    }

    static LegalContractChatRespVO toAgentEventResp(LegalAgentSseEventHolder.SseEvent event, String sessionId) {
        return new LegalContractChatRespVO()
                .setEventType(event.getEventType())
                .setConfirmId(event.getConfirmId())
                .setConfirmSummary(event.getToolSummary())
                .setToolName(event.getToolName())
                .setToolSummary(event.getToolSummary())
                .setProposalNo(event.getProposalNo())
                .setProposalAction(event.getProposalAction())
                .setProposalTitle(event.getProposalTitle())
                .setProposalPayload(event.getProposalPayload())
                .setSessionId(sessionId);
    }
}
