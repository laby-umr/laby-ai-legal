package com.laby.module.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessagePageReqVO;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageRespVO;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import com.laby.module.ai.dal.dataobject.chat.AiChatMessageDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.dal.mysql.chat.AiChatMessageMapper;
import com.laby.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.framework.common.util.collection.CollectionUtils.convertList;
import static com.laby.module.ai.enums.ErrorCodeConstants.CHAT_MESSAGE_NOT_EXIST;

/**
 * AI 聊天消息落库与 CRUD。
 */
@Service
public class AiChatMessagePersistenceService {

    public static final String MESSAGE_TYPE_USER = "user";
    public static final String MESSAGE_TYPE_ASSISTANT = "assistant";

    @Resource
    private AiChatMessageMapper chatMessageMapper;

    public AiChatMessageDO createChatMessage(Long conversationId, Long replyId,
                                             AiModelDO model, Long userId, Long roleId,
                                             String messageType, String content, Boolean useContext,
                                             List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
                                             List<String> attachmentUrls,
                                             AiWebSearchResponse webSearchResponse,
                                             Map<String, Object> recallDiagnostics) {
        AiChatMessageDO message = new AiChatMessageDO().setConversationId(conversationId).setReplyId(replyId)
                .setModel(model.getModel()).setModelId(model.getId()).setUserId(userId).setRoleId(roleId)
                .setType(messageType).setContent(content).setUseContext(useContext)
                .setSegmentIds(convertList(knowledgeSegments, AiKnowledgeSegmentSearchRespBO::getId))
                .setAttachmentUrls(attachmentUrls)
                .setRecallDiagnostics(recallDiagnostics);
        if (webSearchResponse != null) {
            message.setWebSearchPages(webSearchResponse.getLists());
        }
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return message;
    }

    public void updateAssistantContent(Long assistantMessageId, String content, String reasoningContent,
                                       Map<String, Object> recallDiagnostics) {
        chatMessageMapper.updateById(new AiChatMessageDO().setId(assistantMessageId)
                .setContent(content)
                .setReasoningContent(reasoningContent)
                .setRecallDiagnostics(recallDiagnostics));
    }

    public void updateAssistantStreamContent(Long assistantMessageId, String content, String reasoningContent) {
        chatMessageMapper.updateById(new AiChatMessageDO().setId(assistantMessageId)
                .setContent(content)
                .setReasoningContent(reasoningContent));
    }

    public void deleteAssistantMessage(Long assistantMessageId) {
        chatMessageMapper.deleteById(assistantMessageId);
    }

    public List<AiChatMessageRespVO.KnowledgeSegment> buildKnowledgeSegmentViews(
            List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
            Map<Long, AiKnowledgeDocumentDO> documentMap) {
        return BeanUtils.toBean(knowledgeSegments, AiChatMessageRespVO.KnowledgeSegment.class, segment -> {
            AiKnowledgeDocumentDO document = documentMap.get(segment.getDocumentId());
            segment.setDocumentName(document != null ? document.getName() : null);
        });
    }

    public AiChatMessageSendRespVO buildSendResponse(AiChatMessageDO userMessage,
                                                     AiChatMessageDO assistantMessage,
                                                     String assistantContent,
                                                     String reasoningContent,
                                                     List<AiChatMessageRespVO.KnowledgeSegment> segments,
                                                     Map<String, Object> recallDiagnostics,
                                                     AiWebSearchResponse webSearchResponse) {
        return new AiChatMessageSendRespVO()
                .setSend(BeanUtils.toBean(userMessage, AiChatMessageSendRespVO.Message.class))
                .setReceive(BeanUtils.toBean(assistantMessage, AiChatMessageSendRespVO.Message.class)
                        .setContent(assistantContent)
                        .setReasoningContent(reasoningContent)
                        .setSegments(segments)
                        .setRecallDiagnostics(recallDiagnostics)
                        .setWebSearchPages(webSearchResponse != null ? webSearchResponse.getLists() : null));
    }

    public List<AiChatMessageDO> getChatMessageListByConversationId(Long conversationId) {
        return chatMessageMapper.selectListByConversationId(conversationId);
    }

    /**
     * 按时间倒序取最近一条带附件的用户消息 URL 列表；无则空列表。
     */
    public List<String> listLatestUserAttachmentUrls(Long conversationId) {
        List<AiChatMessageDO> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapperX<AiChatMessageDO>()
                        .eq(AiChatMessageDO::getConversationId, conversationId)
                        .eq(AiChatMessageDO::getType, MESSAGE_TYPE_USER)
                        .orderByDesc(AiChatMessageDO::getId));
        for (AiChatMessageDO message : messages) {
            if (CollUtil.isNotEmpty(message.getAttachmentUrls())) {
                return message.getAttachmentUrls();
            }
        }
        return Collections.emptyList();
    }

    public void deleteChatMessage(Long id, Long userId) {
        AiChatMessageDO message = chatMessageMapper.selectById(id);
        if (message == null || ObjUtil.notEqual(message.getUserId(), userId)) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        chatMessageMapper.deleteById(id);
    }

    public void deleteChatMessageByConversationId(Long conversationId, Long userId) {
        List<AiChatMessageDO> messages = chatMessageMapper.selectListByConversationId(conversationId);
        if (CollUtil.isEmpty(messages) || ObjUtil.notEqual(messages.get(0).getUserId(), userId)) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        chatMessageMapper.deleteByIds(convertList(messages, AiChatMessageDO::getId));
    }

    public void deleteChatMessageByAdmin(Long id) {
        AiChatMessageDO message = chatMessageMapper.selectById(id);
        if (message == null) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        chatMessageMapper.deleteById(id);
    }

    public Map<Long, Integer> getChatMessageCountMap(Collection<Long> conversationIds) {
        return chatMessageMapper.selectCountMapByConversationId(conversationIds);
    }

    public PageResult<AiChatMessageDO> getChatMessagePage(AiChatMessagePageReqVO pageReqVO) {
        return chatMessageMapper.selectPage(pageReqVO);
    }

}
