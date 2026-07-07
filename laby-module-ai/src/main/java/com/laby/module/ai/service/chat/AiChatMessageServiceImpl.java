package com.laby.module.ai.service.chat;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessagePageReqVO;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageRespVO;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.laby.module.ai.dal.dataobject.chat.AiChatMessageDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.ErrorCodeConstants;
import com.laby.module.ai.framework.agentscope.session.AiChatConversationStreamGuard;
import com.laby.module.ai.framework.ai.core.webserch.AiWebSearchClient;
import com.laby.module.ai.framework.ai.core.webserch.AiWebSearchRequest;
import com.laby.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import com.laby.module.ai.framework.knowledge.retrieval.NoAnswerGuard;
import com.laby.module.ai.framework.knowledge.retrieval.RecallDiagnosticsConverter;
import com.laby.module.ai.service.chat.bo.AiChatKnowledgeRecallResultBO;
import com.laby.module.ai.service.knowledge.AiKnowledgeDocumentService;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.ai.service.model.AiModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.framework.common.util.collection.CollectionUtils.convertSet;
import static com.laby.module.ai.enums.ErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;

/**
 * AI 聊天消息 Service 实现类（编排层，委托 Persistence / RAG / Stream）。
 */
@Service
@Slf4j
public class AiChatMessageServiceImpl implements AiChatMessageService {

    private static final Integer WEB_SEARCH_COUNT = 10;

    @Resource
    private AiChatConversationService chatConversationService;
    @Resource
    private AiModelService modalService;
    @Resource
    private AiChatConversationStreamGuard conversationStreamGuard;
    @Resource
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private AiChatMessagePersistenceService messagePersistenceService;
    @Resource
    private AiChatMessageRagInjector ragInjector;
    @Resource
    private AiChatMessageStreamHandler streamHandler;
    @Autowired(required = false)
    private NoAnswerGuard noAnswerGuard;
    @Autowired(required = false)
    private AiWebSearchClient webSearchClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatMessageSendRespVO sendMessage(AiChatMessageSendReqVO sendReqVO, Long userId) {
        AiChatConversationDO conversation = chatConversationService
                .validateChatConversationExists(sendReqVO.getConversationId());
        if (ObjUtil.notEqual(conversation.getUserId(), userId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        List<AiChatMessageDO> historyMessages = messagePersistenceService
                .getChatMessageListByConversationId(conversation.getId());
        AiModelDO model = modalService.validateModel(conversation.getModelId());
        AiLlmClient llmClient = modalService.getLlmClient(model.getId());

        AiChatKnowledgeRecallResultBO recallResult = ragInjector.recallKnowledgeForChat(
                sendReqVO.getContent(), conversation);
        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments = recallResult.getSegments();
        Map<String, Object> recallDiagnostics = recallResult.getRecallDiagnostics();
        boolean roleHasKnowledge = ragInjector.hasRoleKnowledge(conversation.getRoleId());

        AiWebSearchResponse webSearchResponse = Boolean.TRUE.equals(sendReqVO.getUseSearch()) && webSearchClient != null
                ? webSearchClient.search(new AiWebSearchRequest().setQuery(sendReqVO.getContent())
                .setSummary(true).setCount(WEB_SEARCH_COUNT)) : null;

        AiChatMessageDO userMessage = messagePersistenceService.createChatMessage(
                conversation.getId(), null, model, userId, conversation.getRoleId(),
                AiChatMessagePersistenceService.MESSAGE_TYPE_USER, sendReqVO.getContent(), sendReqVO.getUseContext(),
                null, sendReqVO.getAttachmentUrls(), null, null);
        AiChatMessageDO assistantMessage = messagePersistenceService.createChatMessage(
                conversation.getId(), userMessage.getId(), model, userId, conversation.getRoleId(),
                AiChatMessagePersistenceService.MESSAGE_TYPE_ASSISTANT, "", sendReqVO.getUseContext(),
                knowledgeSegments, null, webSearchResponse, recallDiagnostics);

        Optional<String> guardReply = noAnswerGuard != null
                ? noAnswerGuard.evaluate(knowledgeSegments, roleHasKnowledge) : Optional.empty();
        String newContent;
        String newReasoningContent = null;
        if (guardReply.isPresent()) {
            newContent = guardReply.get();
            Map<String, Object> guardDiagnostics = RecallDiagnosticsConverter.withNoAnswerGuard(recallDiagnostics, true);
            messagePersistenceService.updateAssistantContent(assistantMessage.getId(), newContent, newReasoningContent,
                    guardDiagnostics);
            recallDiagnostics = guardDiagnostics;
        } else {
            AiLlmRequest request = ragInjector.buildAiLlmRequest(conversation, historyMessages, knowledgeSegments,
                    webSearchResponse, sendReqVO, true);
            if (streamHandler.roleHasTools(conversation.getRoleId())) {
                AiLlmRequest harnessRequest = ragInjector.buildAiLlmRequest(conversation, historyMessages,
                        knowledgeSegments, webSearchResponse, sendReqVO, false);
                newContent = streamHandler.callWithHarnessAgent(conversation, userId, model, harnessRequest);
            } else {
                newContent = llmClient.call(request);
            }
            messagePersistenceService.updateAssistantContent(assistantMessage.getId(), newContent, newReasoningContent,
                    recallDiagnostics);
        }

        Map<Long, AiKnowledgeDocumentDO> documentMap = knowledgeDocumentService.getKnowledgeDocumentMap(
                convertSet(knowledgeSegments, AiKnowledgeSegmentSearchRespBO::getDocumentId));
        List<AiChatMessageRespVO.KnowledgeSegment> segments = messagePersistenceService.buildKnowledgeSegmentViews(
                knowledgeSegments, documentMap);
        return messagePersistenceService.buildSendResponse(userMessage, assistantMessage, newContent, newReasoningContent,
                segments, recallDiagnostics, webSearchResponse);
    }

    @Override
    public Flux<CommonResult<AiChatMessageSendRespVO>> sendChatMessageStream(AiChatMessageSendReqVO sendReqVO,
                                                                               Long userId) {
        AiChatConversationDO conversation = chatConversationService
                .validateChatConversationExists(sendReqVO.getConversationId());
        if (ObjUtil.notEqual(conversation.getUserId(), userId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        if (!conversationStreamGuard.tryAcquire(conversation.getId())) {
            return Flux.just(CommonResult.error(ErrorCodeConstants.CHAT_CONVERSATION_STREAM_BUSY));
        }
        List<AiChatMessageDO> historyMessages = messagePersistenceService
                .getChatMessageListByConversationId(conversation.getId());
        AiModelDO model = modalService.validateModel(conversation.getModelId());
        AiLlmClient llmClient = modalService.getLlmClient(model.getId());

        AiChatKnowledgeRecallResultBO recallResult = ragInjector.recallKnowledgeForChat(
                sendReqVO.getContent(), conversation);
        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments = recallResult.getSegments();
        Map<String, Object> recallDiagnostics = recallResult.getRecallDiagnostics();
        boolean roleHasKnowledge = ragInjector.hasRoleKnowledge(conversation.getRoleId());

        AiWebSearchResponse webSearchResponse = Boolean.TRUE.equals(sendReqVO.getUseSearch()) && webSearchClient != null
                ? webSearchClient.search(new AiWebSearchRequest().setQuery(sendReqVO.getContent())
                .setSummary(true).setCount(WEB_SEARCH_COUNT)) : null;

        AiChatMessageDO userMessage = messagePersistenceService.createChatMessage(
                conversation.getId(), null, model, userId, conversation.getRoleId(),
                AiChatMessagePersistenceService.MESSAGE_TYPE_USER, sendReqVO.getContent(), sendReqVO.getUseContext(),
                null, sendReqVO.getAttachmentUrls(), null, null);
        AiChatMessageDO assistantMessage = messagePersistenceService.createChatMessage(
                conversation.getId(), userMessage.getId(), model, userId, conversation.getRoleId(),
                AiChatMessagePersistenceService.MESSAGE_TYPE_ASSISTANT, "", sendReqVO.getUseContext(),
                knowledgeSegments, null, webSearchResponse, recallDiagnostics);

        Optional<String> guardReply = noAnswerGuard != null
                ? noAnswerGuard.evaluate(knowledgeSegments, roleHasKnowledge) : Optional.empty();
        if (guardReply.isPresent()) {
            String fixedReply = guardReply.get();
            Map<String, Object> guardDiagnostics = RecallDiagnosticsConverter.withNoAnswerGuard(recallDiagnostics, true);
            messagePersistenceService.updateAssistantContent(assistantMessage.getId(), fixedReply, null, guardDiagnostics);
            return Flux.just(success(messagePersistenceService.buildSendResponse(userMessage, assistantMessage,
                            fixedReply, null, Collections.emptyList(), guardDiagnostics, webSearchResponse)))
                    .doFinally(signal -> conversationStreamGuard.release(conversation.getId()));
        }

        return streamHandler.streamChat(conversation, sendReqVO, userId, model, llmClient, historyMessages,
                        knowledgeSegments, recallDiagnostics, webSearchResponse, userMessage, assistantMessage, ragInjector)
                .doFinally(signal -> conversationStreamGuard.release(conversation.getId()));
    }

    @Override
    public List<AiChatMessageDO> getChatMessageListByConversationId(Long conversationId) {
        return messagePersistenceService.getChatMessageListByConversationId(conversationId);
    }

    @Override
    public List<String> listLatestUserAttachmentUrls(Long conversationId) {
        return messagePersistenceService.listLatestUserAttachmentUrls(conversationId);
    }

    @Override
    public void deleteChatMessage(Long id, Long userId) {
        messagePersistenceService.deleteChatMessage(id, userId);
    }

    @Override
    public void deleteChatMessageByConversationId(Long conversationId, Long userId) {
        messagePersistenceService.deleteChatMessageByConversationId(conversationId, userId);
    }

    @Override
    public void deleteChatMessageByAdmin(Long id) {
        messagePersistenceService.deleteChatMessageByAdmin(id);
    }

    @Override
    public Map<Long, Integer> getChatMessageCountMap(Collection<Long> conversationIds) {
        return messagePersistenceService.getChatMessageCountMap(conversationIds);
    }

    @Override
    public PageResult<AiChatMessageDO> getChatMessagePage(AiChatMessagePageReqVO pageReqVO) {
        return messagePersistenceService.getChatMessagePage(pageReqVO);
    }

}
