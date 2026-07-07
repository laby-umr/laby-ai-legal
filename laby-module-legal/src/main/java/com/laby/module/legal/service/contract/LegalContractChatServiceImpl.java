package com.laby.module.legal.service.contract;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.exception.ServiceException;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiLlmStreamEvent;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.enums.contract.LegalContractChatAnswerModeEnum;
import com.laby.module.legal.service.agent.LegalContractAgentPromptHelper;
import com.laby.module.legal.service.agent.LegalContractAgentService;
import com.laby.module.legal.service.ai.LegalAiModelResolver;
import com.laby.module.legal.service.trace.LegalAiTraceRecorder;
import com.laby.module.legal.service.trace.LegalAiTraceRecorder.TraceSession;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_CHAT_CONTEXT_EMPTY;

/**
 * 法务合同问答 Service 实现类
 * <p>
 * 支持普通模式（固定上下文）与 Agent 模式（Tool 按需查数）；多轮消息持久化至数据库。
 */
@Slf4j
@Service
public class LegalContractChatServiceImpl implements LegalContractChatService {

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAiModelResolver legalAiModelResolver;
    @Resource
    private AiModelService aiModelService;
    @Resource
    private LegalContractAgentService contractAgentService;
    @Resource
    private LegalContractAgentPromptHelper agentPromptHelper;
    @Resource
    private LegalContractChatMessageService chatMessageService;
    @Resource
    private LegalAiTraceRecorder aiTraceRecorder;
    @Resource
    private LegalContractChatHistoryHelper chatHistoryHelper;
    @Resource
    private LegalContractChatContextBuilder chatContextBuilder;
    @Resource
    private LegalContractChatRagSupport chatRagSupport;
    @Resource
    private LegalContractChatAgentSupport chatAgentSupport;

    @Override
    public LegalContractChatRespVO chat(LegalContractChatReqVO reqVO) {
        LegalContractDO contract = contractService.validateContractExists(reqVO.getContractId());
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        ensureSessionId(reqVO);
        boolean agentMode = chatAgentSupport.resolveAgentMode(reqVO);
        LegalContractChatMessageService.StreamTurn turn = chatMessageService.beginStreamTurn(
                reqVO.getContractId(), userId, reqVO.getMessage(), agentMode, reqVO.getSessionId());
        List<LegalContractChatMessageDO> history = agentMode
                ? List.of()
                : chatMessageService.listHistoryBefore(
                reqVO.getContractId(), userId, reqVO.getSessionId(), turn.userMessage().getId());
        AiModelDO model = legalAiModelResolver.resolveChatModel(contract.getModelId());
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        TraceSession traceSession = aiTraceRecorder.startChat(
                contract.getId(), model.getId(), platform.getPlatform());
        try {
            String content;
            String reasoningContent = null;
            if (agentMode) {
                LegalContractChatRespVO agentResp = contractAgentService.runSync(contract, reqVO);
                content = agentResp.getContent();
                reasoningContent = agentResp.getReasoningContent();
            } else {
                AiLlmClient llmClient = aiModelService.getLlmClient(model.getId());
                LegalContractChatAnswerModeEnum answerMode = LegalContractChatAnswerModeEnum.of(reqVO.getAnswerMode());
                AiLlmRequest request = buildAiLlmRequest(contract, reqVO, answerMode, model, history, false);
                content = llmClient.call(request);
            }
            chatMessageService.finalizeAssistantMessage(turn.assistantMessage().getId(), content, reasoningContent);
            aiTraceRecorder.completeChat(traceSession);
            return new LegalContractChatRespVO()
                    .setContent(content)
                    .setReasoningContent(reasoningContent)
                    .setSessionId(reqVO.getSessionId())
                    .setUserMessageId(turn.userMessage().getId())
                    .setAssistantMessageId(turn.assistantMessage().getId());
        } catch (Exception ex) {
            aiTraceRecorder.fail(traceSession, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public Flux<CommonResult<LegalContractChatRespVO>> chatStream(LegalContractChatReqVO reqVO) {
        try {
            LegalContractDO contract = contractService.validateContractExists(reqVO.getContractId());
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            ensureSessionId(reqVO);
            boolean agentMode = chatAgentSupport.resolveAgentMode(reqVO);
            LegalContractChatMessageService.StreamTurn turn = chatMessageService.beginStreamTurn(
                    reqVO.getContractId(), userId, reqVO.getMessage(), agentMode, reqVO.getSessionId());
            List<LegalContractChatMessageDO> history = agentMode
                    ? List.of()
                    : chatMessageService.listHistoryBefore(
                    reqVO.getContractId(), userId, reqVO.getSessionId(), turn.userMessage().getId());

            AiModelDO model = legalAiModelResolver.resolveChatModel(contract.getModelId());
            AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
            TraceSession traceSession = aiTraceRecorder.startChat(
                    contract.getId(), model.getId(), platform.getPlatform());

            Flux<CommonResult<LegalContractChatRespVO>> stream = agentMode
                    ? contractAgentService.runStream(contract, reqVO)
                    : chatStreamNormal(contract, reqVO, history);

            AtomicReference<String> streamError = new AtomicReference<>();
            return wrapStreamPersistence(stream, turn, reqVO.getSessionId())
                    .onErrorResume(ex -> Flux.just(chatStreamErrorResult(reqVO.getSessionId(), ex)))
                    .doOnError(ex -> streamError.set(ex.getMessage()))
                    .doFinally(signal -> {
                        if (signal == SignalType.ON_COMPLETE) {
                            aiTraceRecorder.completeChat(traceSession);
                        } else if (signal == SignalType.ON_ERROR) {
                            aiTraceRecorder.fail(traceSession,
                                    StrUtil.blankToDefault(streamError.get(), "stream error"));
                        } else if (signal == SignalType.CANCEL) {
                            aiTraceRecorder.fail(traceSession, "stream cancelled");
                        }
                        chatAgentSupport.cleanupAgentStream(reqVO.getSessionId(), signal);
                    });
        } catch (ServiceException ex) {
            return Flux.<CommonResult<LegalContractChatRespVO>>just(CommonResult.error(ex))
                    .doFinally(signal -> chatAgentSupport.cleanupAgentStream(reqVO.getSessionId(), signal));
        } catch (Exception ex) {
            log.warn("[chatStream] unexpected error", ex);
            return Flux.<CommonResult<LegalContractChatRespVO>>just(
                            CommonResult.error(500, "问答失败，请稍后重试"))
                    .doFinally(signal -> chatAgentSupport.cleanupAgentStream(reqVO.getSessionId(), signal));
        }
    }

    private Flux<CommonResult<LegalContractChatRespVO>> chatStreamNormal(LegalContractDO contract,
                                                                         LegalContractChatReqVO reqVO,
                                                                         List<LegalContractChatMessageDO> history) {
        AiModelDO model = legalAiModelResolver.resolveChatModel(contract.getModelId());
        AiLlmClient llmClient = aiModelService.getLlmClient(model.getId());
        LegalContractChatAnswerModeEnum answerMode = LegalContractChatAnswerModeEnum.of(reqVO.getAnswerMode());
        AiLlmRequest request = buildAiLlmRequest(contract, reqVO, answerMode, model, history, false);

        Flux<CommonResult<LegalContractChatRespVO>> start = Flux.just(success(new LegalContractChatRespVO()
                .setSessionId(reqVO.getSessionId())));

        Flux<CommonResult<LegalContractChatRespVO>> stream = llmClient.stream(request)
                .flatMap(event -> {
                    if (event.getType() == AiLlmStreamEvent.Type.ERROR) {
                        return Flux.just(chatStreamErrorResult(reqVO.getSessionId(),
                                new RuntimeException(event.getErrorMessage())));
                    }
                    if (event.getType() == AiLlmStreamEvent.Type.DONE) {
                        return Flux.empty();
                    }
                    if (event.getType() == AiLlmStreamEvent.Type.CONTENT) {
                        return Flux.just(success(new LegalContractChatRespVO()
                                .setContent(StrUtil.nullToDefault(event.getDelta(), ""))
                                .setSessionId(reqVO.getSessionId())));
                    }
                    if (event.getType() == AiLlmStreamEvent.Type.REASONING) {
                        return Flux.just(success(new LegalContractChatRespVO()
                                .setReasoningContent(StrUtil.nullToDefault(event.getDelta(), ""))
                                .setSessionId(reqVO.getSessionId())));
                    }
                    return Flux.empty();
                })
                .onErrorResume(ex -> Flux.just(chatStreamErrorResult(reqVO.getSessionId(), ex)));

        return Flux.concat(start, stream);
    }

    private AiLlmRequest buildAiLlmRequest(LegalContractDO contract, LegalContractChatReqVO reqVO,
                                           LegalContractChatAnswerModeEnum answerMode, AiModelDO model,
                                           List<LegalContractChatMessageDO> history, boolean agentMode) {
        int maxTokens = chatContextBuilder.resolveChatMaxTokens(answerMode.getMaxTokens(), contract);
        List<AiMessage> messages = new ArrayList<>();

        if (agentMode) {
            boolean allowProposal = Boolean.TRUE.equals(reqVO.getAllowProposal());
            messages.add(new AiMessage().setRole(AiMessageRoleEnum.SYSTEM)
                    .setContent(agentPromptHelper.buildSystemPrompt(contract, answerMode, allowProposal)));
        } else {
            String context = chatContextBuilder.buildContractContext(contract);
            if (StrUtil.isBlank(context)) {
                throw exception(CONTRACT_CHAT_CONTEXT_EMPTY);
            }
            messages.add(new AiMessage().setRole(AiMessageRoleEnum.SYSTEM)
                    .setContent(LegalContractChatContextBuilder.buildSystemPrompt(answerMode)));
            messages.add(new AiMessage().setRole(AiMessageRoleEnum.SYSTEM).setContent("【合同上下文】\n" + context));
            chatRagSupport.appendKnowledgeContext(messages, contract, reqVO.getMessage());
        }

        chatHistoryHelper.appendAiHistory(messages, history);
        messages.add(new AiMessage().setRole(AiMessageRoleEnum.USER).setContent(reqVO.getMessage().trim()));

        return new AiLlmRequest()
                .setMessages(messages)
                .setTemperature(model.getTemperature())
                .setMaxTokens(maxTokens);
    }

    private Flux<CommonResult<LegalContractChatRespVO>> wrapStreamPersistence(
            Flux<CommonResult<LegalContractChatRespVO>> stream,
            LegalContractChatMessageService.StreamTurn turn,
            String sessionId) {
        StringBuilder contentBuffer = new StringBuilder();
        StringBuilder reasoningBuffer = new StringBuilder();
        AtomicReference<Boolean> startSent = new AtomicReference<>(false);

        return stream
                .map(result -> {
                    if (Boolean.FALSE.equals(startSent.get())) {
                        startSent.set(true);
                        if (result.getData() != null) {
                            result.getData()
                                    .setUserMessageId(turn.userMessage().getId())
                                    .setAssistantMessageId(turn.assistantMessage().getId())
                                    .setSessionId(sessionId);
                        }
                    }
                    accumulateStreamContent(result, contentBuffer, reasoningBuffer);
                    return result;
                })
                .doFinally(signal -> TenantUtils.executeIgnore(() ->
                        persistStreamResult(turn.assistantMessage().getId(), contentBuffer, reasoningBuffer, signal)));
    }

    private static void accumulateStreamContent(CommonResult<LegalContractChatRespVO> result,
                                                StringBuilder contentBuffer,
                                                StringBuilder reasoningBuffer) {
        if (result == null || result.getData() == null) {
            return;
        }
        LegalContractChatRespVO data = result.getData();
        if (StrUtil.isNotEmpty(data.getContent())) {
            if ("error".equals(data.getEventType())) {
                if (contentBuffer.isEmpty()) {
                    contentBuffer.append(data.getContent());
                }
            } else if (data.getEventType() == null) {
                contentBuffer.append(data.getContent());
            }
        }
        if (StrUtil.isNotEmpty(data.getReasoningContent())) {
            reasoningBuffer.append(data.getReasoningContent());
        }
    }

    private void persistStreamResult(Long assistantMessageId, StringBuilder contentBuffer,
                                     StringBuilder reasoningBuffer, SignalType signal) {
        String content = contentBuffer.toString();
        String reasoning = reasoningBuffer.toString();
        if (StrUtil.isNotBlank(content) || StrUtil.isNotBlank(reasoning)) {
            chatMessageService.finalizeAssistantMessage(assistantMessageId, content, reasoning);
            return;
        }
        if (signal == SignalType.CANCEL) {
            chatMessageService.deleteAssistantIfEmpty(assistantMessageId);
        }
    }

    private static CommonResult<LegalContractChatRespVO> chatStreamErrorResult(String sessionId, Throwable ex) {
        return success(new LegalContractChatRespVO()
                .setEventType("error")
                .setContent(resolveChatStreamErrorMessage(ex))
                .setSessionId(sessionId));
    }

    private static String resolveChatStreamErrorMessage(Throwable ex) {
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return "问答超时，请稍后重试";
        }
        String message = StrUtil.blankToDefault(ex.getMessage(), "未知错误");
        if (message.contains("Connection reset") || message.contains("Retries exhausted")) {
            return "模型服务连接中断，请检查 API 地址/密钥或稍后重试";
        }
        return "问答失败，请稍后重试";
    }

    private static void ensureSessionId(LegalContractChatReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getSessionId())) {
            reqVO.setSessionId(IdUtil.fastSimpleUUID());
        }
    }

}
