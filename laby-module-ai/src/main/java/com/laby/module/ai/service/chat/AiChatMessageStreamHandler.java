package com.laby.module.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.exception.ErrorCode;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageRespVO;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiLlmStreamEvent;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.laby.module.ai.dal.dataobject.chat.AiChatMessageDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.dal.dataobject.model.AiToolDO;
import com.laby.module.ai.enums.ErrorCodeConstants;
import com.laby.module.ai.framework.agentscope.chat.AiChatAgentScopeConfig;
import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummaryContext;
import com.laby.module.ai.framework.agentscope.chat.AiChatHarnessSupport;
import com.laby.module.ai.framework.agentscope.chat.AiChatToolRegistry;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.framework.agentscope.model.AiMessageConverter;
import com.laby.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import com.laby.module.ai.service.knowledge.AiKnowledgeDocumentService;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.ai.service.model.AiApiKeyService;
import com.laby.module.ai.service.model.AiChatRoleService;
import com.laby.module.ai.service.model.AiToolService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.laby.framework.common.pojo.CommonResult.error;
import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.framework.common.util.collection.CollectionUtils.convertList;
import static com.laby.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * AI 聊天 SSE 流式响应与 Harness Agent 流转换。
 */
@Slf4j
@Component
public class AiChatMessageStreamHandler {

    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private AiApiKeyService apiKeyService;
    @Resource
    private AiToolService toolService;
    @Resource
    private AiChatAgentScopeConfig aiChatAgentScopeConfig;
    @Resource
    private AiChatToolRegistry aiChatToolRegistry;
    @Autowired(required = false)
    private List<AiChatHarnessSupport> chatHarnessSupports = Collections.emptyList();
    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private AiChatMessagePersistenceService messagePersistenceService;

    public Flux<CommonResult<AiChatMessageSendRespVO>> streamChat(
            AiChatConversationDO conversation,
            AiChatMessageSendReqVO sendReqVO,
            Long userId,
            AiModelDO model,
            AiLlmClient llmClient,
            List<AiChatMessageDO> historyMessages,
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            Map<String, Object> recallDiagnostics,
            AiWebSearchResponse webSearchResponse,
            AiChatMessageDO userMessage,
            AiChatMessageDO assistantMessage,
            AiChatMessageRagInjector ragInjector) {

        AiLlmRequest request = ragInjector.buildAiLlmRequest(conversation, historyMessages, knowledgeSegments,
                webSearchResponse, sendReqVO, true);
        Flux<AiLlmStreamEvent> streamResponse;
        if (roleHasTools(conversation.getRoleId())) {
            RuntimeContext runtimeContext = buildChatRuntimeContext(conversation, userId, model);
            HarnessAgent agent = buildChatHarnessAgent(model, conversation, runtimeContext);
            AiLlmRequest harnessRequest = ragInjector.buildAiLlmRequest(conversation, historyMessages, knowledgeSegments,
                    webSearchResponse, sendReqVO, false);
            List<Msg> agentMessages = convertAiMessagesToAgentScopeMsgs(harnessRequest.getMessages());
            streamResponse = agent.streamEvents(agentMessages, runtimeContext)
                    .flatMap(event -> {
                        AiLlmStreamEvent mapped = mapAgentEventToLlmStreamEvent(event);
                        return mapped != null ? Flux.just(mapped) : Flux.empty();
                    })
                    .onErrorResume(ex -> Flux.just(new AiLlmStreamEvent()
                            .setType(AiLlmStreamEvent.Type.ERROR)
                            .setErrorMessage(resolveAgentStreamErrorMessage(ex))))
                    .concatWith(Flux.just(new AiLlmStreamEvent().setType(AiLlmStreamEvent.Type.DONE)));
        } else {
            streamResponse = llmClient.stream(request);
        }

        StringBuffer contentBuffer = new StringBuffer();
        StringBuffer reasoningContentBuffer = new StringBuffer();
        AtomicBoolean firstExecuteFlag = new AtomicBoolean(true);
        AtomicReference<List<AiChatMessageRespVO.KnowledgeSegment>> cacheSegments = new AtomicReference<>();
        AtomicReference<List<AiWebSearchResponse.WebPage>> cacheWebSearchPages = new AtomicReference<>();
        AtomicReference<Map<String, Object>> cacheRecallDiagnostics = new AtomicReference<>(recallDiagnostics);

        return streamResponse.flatMap(event -> {
            if (event.getType() == AiLlmStreamEvent.Type.ERROR) {
                return Flux.error(new RuntimeException(event.getErrorMessage()));
            }
            if (event.getType() == AiLlmStreamEvent.Type.DONE) {
                return Flux.empty();
            }
            if (StrUtil.isEmpty(contentBuffer)) {
                if (firstExecuteFlag.compareAndSet(true, false)) {
                    Map<Long, AiKnowledgeDocumentDO> documentMap = TenantUtils.executeIgnore(() ->
                            knowledgeDocumentService.getKnowledgeDocumentMap(
                                    convertSet(knowledgeSegments, AiKnowledgeSegmentSearchRespBO::getDocumentId)));
                    cacheSegments.set(BeanUtils.toBean(knowledgeSegments, AiChatMessageRespVO.KnowledgeSegment.class, segment -> {
                        AiKnowledgeDocumentDO document = documentMap.get(segment.getDocumentId());
                        segment.setDocumentName(document != null ? document.getName() : null);
                    }));
                    if (webSearchResponse != null) {
                        cacheWebSearchPages.set(webSearchResponse.getLists());
                    }
                }
            }
            String newContent = getStreamEventContent(event);
            String newReasoningContent = getStreamEventReasoningContent(event);
            if (StrUtil.isEmpty(newContent) && StrUtil.isEmpty(newReasoningContent)) {
                return Flux.empty();
            }
            if (StrUtil.isNotEmpty(newContent)) {
                contentBuffer.append(newContent);
            }
            if (StrUtil.isNotEmpty(newReasoningContent)) {
                reasoningContentBuffer.append(newReasoningContent);
            }
            return Flux.just(success(new AiChatMessageSendRespVO()
                    .setSend(BeanUtils.toBean(userMessage, AiChatMessageSendRespVO.Message.class))
                    .setReceive(BeanUtils.toBean(assistantMessage, AiChatMessageSendRespVO.Message.class)
                            .setContent(StrUtil.nullToDefault(newContent, ""))
                            .setReasoningContent(StrUtil.nullToDefault(newReasoningContent, ""))
                            .setSegments(cacheSegments.get()).setWebSearchPages(cacheWebSearchPages.get())
                            .setRecallDiagnostics(cacheRecallDiagnostics.get()))));
        }).doOnComplete(() -> TenantUtils.executeIgnore(() ->
                messagePersistenceService.updateAssistantContent(assistantMessage.getId(),
                        contentBuffer.toString(), reasoningContentBuffer.toString(), cacheRecallDiagnostics.get())))
                .doOnError(throwable -> {
                    log.error("[streamChat][userId({}) sendReqVO({}) 发生异常]", userId, sendReqVO, throwable);
                    TenantUtils.executeIgnore(() -> {
                        if (StrUtil.isNotEmpty(contentBuffer)) {
                            messagePersistenceService.updateAssistantStreamContent(assistantMessage.getId(),
                                    contentBuffer.toString(), reasoningContentBuffer.toString());
                        } else {
                            messagePersistenceService.deleteAssistantMessage(assistantMessage.getId());
                        }
                    });
                }).doOnCancel(() -> {
                    log.info("[streamChat][userId({}) sendReqVO({}) 取消请求]", userId, sendReqVO);
                    TenantUtils.executeIgnore(() -> {
                        if (StrUtil.isNotEmpty(contentBuffer)) {
                            messagePersistenceService.updateAssistantStreamContent(assistantMessage.getId(),
                                    contentBuffer.toString(), reasoningContentBuffer.toString());
                        } else {
                            messagePersistenceService.deleteAssistantMessage(assistantMessage.getId());
                        }
                    });
                }).onErrorResume(streamError -> Flux.just(error(resolveStreamErrorCode(streamError))));
    }

    public String callWithHarnessAgent(AiChatConversationDO conversation, Long userId, AiModelDO model,
                                       AiLlmRequest harnessRequest) {
        RuntimeContext runtimeContext = buildChatRuntimeContext(conversation, userId, model);
        HarnessAgent agent = buildChatHarnessAgent(model, conversation, runtimeContext);
        List<Msg> agentMessages = convertAiMessagesToAgentScopeMsgs(harnessRequest.getMessages());
        return agent.call(agentMessages, runtimeContext).block().getTextContent();
    }

    public boolean roleHasTools(Long roleId) {
        if (roleId == null) {
            return false;
        }
        AiChatRoleDO chatRole = chatRoleService.getChatRole(roleId);
        return chatRole != null
                && (CollUtil.isNotEmpty(chatRole.getToolIds()) || CollUtil.isNotEmpty(chatRole.getMcpClientNames()));
    }

    private List<String> resolveRoleToolBeanNames(Long roleId) {
        AiChatRoleDO chatRole = chatRoleService.getChatRole(roleId);
        if (chatRole == null || CollUtil.isEmpty(chatRole.getToolIds())) {
            return Collections.emptyList();
        }
        List<AiToolDO> tools = toolService.getToolList(chatRole.getToolIds());
        return convertList(tools, AiToolDO::getName);
    }

    private HarnessAgent buildChatHarnessAgent(AiModelDO model, AiChatConversationDO conversation,
                                                RuntimeContext runtimeContext) {
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        String sessionId = String.valueOf(conversation.getId());
        Path workspace = Paths.get(agentScopeProperties.getWorkspacePath(), "chat", sessionId);
        Toolkit toolkit = aiChatToolRegistry.buildToolkit(
                resolveRoleToolBeanNames(conversation.getRoleId()),
                resolveRoleMcpClientNames(conversation.getRoleId()),
                workspace);
        List<MiddlewareBase> extraMiddlewares = resolveExtraMiddlewares(conversation);
        ToolExecutionContext toolExecutionContext = runtimeContext != null
                ? runtimeContext.getToolExecutionContext() : null;
        if (toolExecutionContext == null && runtimeContext != null) {
            toolExecutionContext = runtimeContext.asToolExecutionContext();
        }
        return aiChatAgentScopeConfig.buildAgent(model, apiKey, toolkit,
                conversation.getSystemMessage(), sessionId, extraMiddlewares, toolExecutionContext);
    }

    private List<MiddlewareBase> resolveExtraMiddlewares(AiChatConversationDO conversation) {
        List<String> toolNames = resolveRoleToolBeanNames(conversation.getRoleId());
        List<MiddlewareBase> middlewares = new ArrayList<>();
        for (AiChatHarnessSupport support : chatHarnessSupports) {
            if (support.supports(conversation.getRoleId(), toolNames)) {
                middlewares.addAll(support.extraMiddlewares(
                        conversation.getId(), conversation.getUserId(), conversation.getRoleId()));
            }
        }
        return middlewares;
    }

    private List<String> resolveRoleMcpClientNames(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        AiChatRoleDO chatRole = chatRoleService.getChatRole(roleId);
        if (chatRole == null || CollUtil.isEmpty(chatRole.getMcpClientNames())) {
            return Collections.emptyList();
        }
        return chatRole.getMcpClientNames();
    }

    private RuntimeContext buildChatRuntimeContext(AiChatConversationDO conversation, Long userId, AiModelDO model) {
        List<String> toolNames = resolveRoleToolBeanNames(conversation.getRoleId());
        RuntimeContext.Builder builder = RuntimeContext.builder()
                .sessionId(String.valueOf(conversation.getId()))
                .put(AiChatCompactionSummaryContext.class,
                        AiChatCompactionSummaryContext.of(conversation, model));
        for (AiChatHarnessSupport support : chatHarnessSupports) {
            if (support.supports(conversation.getRoleId(), toolNames)) {
                return support.enrichRuntime(builder, conversation.getId(), userId, conversation.getRoleId());
            }
        }
        return builder.build();
    }

    private List<Msg> convertAiMessagesToAgentScopeMsgs(List<AiMessage> messages) {
        List<AiMessage> withoutSystem = messages.stream()
                .filter(message -> message.getRole() != AiMessageRoleEnum.SYSTEM)
                .collect(Collectors.toList());
        return AiMessageConverter.toAgentScopeMessages(withoutSystem);
    }

    private static AiLlmStreamEvent mapAgentEventToLlmStreamEvent(AgentEvent event) {
        if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
            return new AiLlmStreamEvent()
                    .setType(AiLlmStreamEvent.Type.CONTENT)
                    .setDelta(((TextBlockDeltaEvent) event).getDelta());
        }
        if (event.getType() == AgentEventType.THINKING_BLOCK_DELTA) {
            return new AiLlmStreamEvent()
                    .setType(AiLlmStreamEvent.Type.REASONING)
                    .setDelta(((ThinkingBlockDeltaEvent) event).getDelta());
        }
        return null;
    }

    private static String getStreamEventContent(AiLlmStreamEvent event) {
        if (event != null && event.getType() == AiLlmStreamEvent.Type.CONTENT) {
            return event.getDelta();
        }
        return null;
    }

    private static String getStreamEventReasoningContent(AiLlmStreamEvent event) {
        if (event != null && event.getType() == AiLlmStreamEvent.Type.REASONING) {
            return event.getDelta();
        }
        return null;
    }

    private static ErrorCode resolveStreamErrorCode(Throwable streamError) {
        if (streamError instanceof WebClientResponseException webEx
                && webEx.getStatusCode() != null
                && webEx.getStatusCode().value() == 401) {
            return ErrorCodeConstants.CHAT_STREAM_MODEL_AUTH_ERROR;
        }
        Throwable cause = streamError.getCause();
        while (cause != null) {
            if (cause instanceof WebClientResponseException webEx
                    && webEx.getStatusCode() != null
                    && webEx.getStatusCode().value() == 401) {
                return ErrorCodeConstants.CHAT_STREAM_MODEL_AUTH_ERROR;
            }
            cause = cause.getCause();
        }
        return ErrorCodeConstants.CHAT_STREAM_ERROR;
    }

    private static String resolveAgentStreamErrorMessage(Throwable ex) {
        String message = StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
        if (message.contains("Agent is still running")) {
            return "Agent 正在处理上一条消息，请稍后再试";
        }
        if (message.contains("Connection reset") || message.contains("Retries exhausted")) {
            return "模型服务连接中断，请检查 API 地址/密钥或稍后重试";
        }
        return "Agent 问答失败：" + StrUtil.sub(message, 0, 200);
    }

}
