package com.laby.module.ai.service.chat;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.google.common.collect.Maps;
import com.laby.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.ai.core.memory.AgentMemoryPolicy;
import com.laby.module.ai.core.token.AiTokenCounter;
import com.laby.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.laby.module.ai.dal.dataobject.chat.AiChatMessageDO;
import com.laby.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.framework.knowledge.retrieval.KnowledgeRetrievalProperties;
import com.laby.module.ai.framework.knowledge.retrieval.RecallDiagnosticsConverter;
import com.laby.module.ai.service.chat.bo.AiChatKnowledgeRecallResultBO;
import com.laby.module.ai.service.knowledge.AiKnowledgeDocumentService;
import com.laby.module.ai.service.knowledge.AiKnowledgeSegmentService;
import com.laby.module.ai.service.knowledge.AiKnowledgeService;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchResultBO;
import com.laby.module.ai.service.model.AiChatRoleService;
import com.laby.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import com.laby.module.ai.util.FileTypeUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天 RAG 召回与 LLM 消息上下文注入（知识库、联网搜索、附件、历史）。
 */
@Slf4j
@Component
public class AiChatMessageRagInjector {

    private static final String KNOWLEDGE_USER_MESSAGE_TEMPLATE = "使用 <Reference></Reference> 标记中的内容作为本次对话的参考:\n\n" +
            "%s\n\n" +
            "回答要求：\n- 避免提及你是从 <Reference></Reference> 获取的知识。\n" +
            "- 若参考内容不足以回答，请明确说「根据知识库内容无法确定」。";

    private static final String WEB_SEARCH_USER_MESSAGE_TEMPLATE = "使用 <WebSearch></WebSearch> 标记中的内容作为本次对话的参考:\n\n" +
            "%s\n\n" +
            "回答要求：\n- 避免提及你是从 <WebSearch></WebSearch> 获取的知识。";

    @SuppressWarnings("TextBlockMigration")
    private static final String ATTACHMENT_USER_MESSAGE_TEMPLATE = "使用 <Attachment></Attachment> 标记用户对话上传的附件内容:\n\n" +
            "%s\n\n" +
            "回答要求：\n- 避免提及 <Attachment></Attachment> 附件的编码格式。";

    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private AiKnowledgeService knowledgeService;
    @Resource
    private AiKnowledgeSegmentService knowledgeSegmentService;
    @Resource
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private AgentMemoryPolicy agentMemoryPolicy;
    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Autowired(required = false)
    private KnowledgeRetrievalProperties knowledgeRetrievalProperties;

    public boolean hasRoleKnowledge(Long roleId) {
        if (roleId == null) {
            return false;
        }
        AiChatRoleDO role = chatRoleService.getChatRole(roleId);
        return role != null && CollUtil.isNotEmpty(role.getKnowledgeIds());
    }

    public AiChatKnowledgeRecallResultBO recallKnowledgeForChat(String content, AiChatConversationDO conversation) {
        if (conversation == null || conversation.getRoleId() == null) {
            return new AiChatKnowledgeRecallResultBO();
        }
        AiChatRoleDO role = chatRoleService.getChatRole(conversation.getRoleId());
        if (role == null || CollUtil.isEmpty(role.getKnowledgeIds())) {
            return new AiChatKnowledgeRecallResultBO();
        }

        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments = new ArrayList<>();
        List<Map<String, Object>> diagnosticItems = new ArrayList<>();
        for (Long knowledgeId : role.getKnowledgeIds()) {
            if (knowledgeService.getKnowledge(knowledgeId) == null) {
                log.warn("[recallKnowledgeForChat][roleId({}) knowledgeId({}) 不存在或已删除，已跳过]",
                        role.getId(), knowledgeId);
                continue;
            }
            AiKnowledgeSegmentSearchResultBO searchResult = knowledgeSegmentService.searchKnowledgeSegmentWithDiagnostics(
                    new AiKnowledgeSegmentSearchReqBO().setKnowledgeId(knowledgeId).setContent(content));
            if (CollUtil.isNotEmpty(searchResult.getSegments())) {
                knowledgeSegments.addAll(searchResult.getSegments());
            }
            if (shouldStoreRecallDiagnostics() && searchResult.getDiagnostics() != null) {
                Map<String, Object> item = RecallDiagnosticsConverter.toMap(searchResult.getDiagnostics());
                if (item != null) {
                    item.put("knowledgeId", knowledgeId);
                    diagnosticItems.add(item);
                }
            }
        }
        Map<String, Object> recallDiagnostics = shouldStoreRecallDiagnostics()
                ? RecallDiagnosticsConverter.mergeChatDiagnostics(diagnosticItems, knowledgeSegments.size())
                : null;
        return new AiChatKnowledgeRecallResultBO()
                .setSegments(knowledgeSegments)
                .setRecallDiagnostics(recallDiagnostics);
    }

    public AiLlmRequest buildAiLlmRequest(AiChatConversationDO conversation, List<AiChatMessageDO> messages,
                                          List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
                                          AiWebSearchResponse webSearchResponse,
                                          AiChatMessageSendReqVO sendReqVO,
                                          boolean includeHistory) {
        List<AiMessage> chatMessages = new ArrayList<>();
        if (StrUtil.isNotBlank(conversation.getSystemMessage())) {
            chatMessages.add(new AiMessage().setRole(AiMessageRoleEnum.SYSTEM).setContent(conversation.getSystemMessage()));
        }

        if (includeHistory) {
            List<AiChatMessageDO> contextMessages = filterContextMessages(messages, conversation, sendReqVO);
            contextMessages.forEach(message -> {
                chatMessages.add(buildHistoryMessage(message.getType(), message.getContent()));
                AiMessage attachmentUserMessage = buildAttachmentUserMessage(message.getAttachmentUrls());
                if (attachmentUserMessage != null) {
                    chatMessages.add(attachmentUserMessage);
                }
            });
        }

        chatMessages.add(new AiMessage().setRole(AiMessageRoleEnum.USER).setContent(sendReqVO.getContent()));

        if (CollUtil.isNotEmpty(knowledgeSegments)) {
            String reference = knowledgeSegments.stream()
                    .map(segment -> "<Reference>" + segment.getRetrievalContent() + "</Reference>")
                    .collect(Collectors.joining("\n\n"));
            chatMessages.add(new AiMessage().setRole(AiMessageRoleEnum.USER)
                    .setContent(String.format(KNOWLEDGE_USER_MESSAGE_TEMPLATE, reference)));
        }

        if (webSearchResponse != null && CollUtil.isNotEmpty(webSearchResponse.getLists())) {
            String webSearch = webSearchResponse.getLists().stream()
                    .map(page -> {
                        String summary = StrUtil.isNotEmpty(page.getSummary()) ?
                                "\nSummary: " + page.getSummary() : "";
                        return "<WebSearch title=\"" + page.getTitle() + "\" url=\"" + page.getUrl() + "\">"
                                + StrUtil.blankToDefault(page.getSummary(), page.getSnippet()) + "</WebSearch>";
                    })
                    .collect(Collectors.joining("\n\n"));
            chatMessages.add(new AiMessage().setRole(AiMessageRoleEnum.USER)
                    .setContent(String.format(WEB_SEARCH_USER_MESSAGE_TEMPLATE, webSearch)));
        }

        if (CollUtil.isNotEmpty(sendReqVO.getAttachmentUrls())) {
            AiMessage attachmentUserMessage = buildAttachmentUserMessage(sendReqVO.getAttachmentUrls());
            if (attachmentUserMessage != null) {
                chatMessages.add(attachmentUserMessage);
            }
        }

        return new AiLlmRequest()
                .setMessages(chatMessages)
                .setTemperature(conversation.getTemperature())
                .setMaxTokens(conversation.getMaxTokens());
    }

    private boolean shouldStoreRecallDiagnostics() {
        return knowledgeRetrievalProperties != null
                && knowledgeRetrievalProperties.getDiagnostics().isEnabled();
    }

    private List<AiChatMessageDO> filterContextMessages(List<AiChatMessageDO> messages,
                                                        AiChatConversationDO conversation,
                                                        AiChatMessageSendReqVO sendReqVO) {
        if (conversation.getMaxContexts() == null || ObjUtil.notEqual(sendReqVO.getUseContext(), Boolean.TRUE)) {
            return Collections.emptyList();
        }
        List<AiChatMessageDO> contextMessages = new ArrayList<>(conversation.getMaxContexts() * 2);
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessageDO assistantMessage = CollUtil.get(messages, i);
            if (assistantMessage == null || assistantMessage.getReplyId() == null
                    || AiChatCompactionSummaryService.MESSAGE_TYPE_SUMMARY.equals(assistantMessage.getType())) {
                continue;
            }
            AiChatMessageDO userMessage = CollUtil.get(messages, i - 1);
            if (userMessage == null
                    || ObjUtil.notEqual(assistantMessage.getReplyId(), userMessage.getId())
                    || StrUtil.isEmpty(assistantMessage.getContent())) {
                continue;
            }
            contextMessages.add(assistantMessage);
            contextMessages.add(userMessage);
            if (contextMessages.size() >= conversation.getMaxContexts() * 2) {
                break;
            }
        }
        Collections.reverse(contextMessages);
        return trimContextMessagesByTokenBudget(contextMessages);
    }

    private List<AiChatMessageDO> trimContextMessagesByTokenBudget(List<AiChatMessageDO> contextMessages) {
        if (CollUtil.isEmpty(contextMessages)) {
            return contextMessages;
        }
        int tokenBudget = agentScopeProperties.getHistoryTokenBudget();
        if (tokenBudget <= 0) {
            return contextMessages;
        }
        return agentMemoryPolicy.trimTail(
                contextMessages,
                tokenBudget,
                0,
                AiChatMessageDO::getContent,
                message -> AiTokenCounter.estimate(message.getContent()));
    }

    private static AiMessage buildHistoryMessage(String type, String content) {
        AiMessageRoleEnum role = switch (type) {
            case AiChatMessagePersistenceService.MESSAGE_TYPE_USER -> AiMessageRoleEnum.USER;
            case AiChatMessagePersistenceService.MESSAGE_TYPE_ASSISTANT -> AiMessageRoleEnum.ASSISTANT;
            case "system" -> AiMessageRoleEnum.SYSTEM;
            default -> throw new IllegalArgumentException(StrUtil.format("未知消息类型({})", type));
        };
        return new AiMessage().setRole(role).setContent(content);
    }

    private AiMessage buildAttachmentUserMessage(List<String> attachmentUrls) {
        if (CollUtil.isEmpty(attachmentUrls)) {
            return null;
        }
        Map<String, String> attachmentContents = Maps.newLinkedHashMapWithExpectedSize(attachmentUrls.size());
        for (String attachmentUrl : attachmentUrls) {
            try {
                String name = FileNameUtil.getName(attachmentUrl);
                String mineType = FileTypeUtils.getMineType(name);
                String content;
                if (FileTypeUtils.isImage(mineType)) {
                    byte[] bytes = HttpUtil.downloadBytes(attachmentUrl);
                    content = Base64.encode(bytes);
                } else {
                    content = knowledgeDocumentService.readUrl(attachmentUrl);
                }
                if (StrUtil.isNotEmpty(content)) {
                    attachmentContents.put(name, content);
                }
            } catch (Exception e) {
                log.error("[buildAttachmentUserMessage][读取附件({}) 发生异常]", attachmentUrl, e);
            }
        }
        if (CollUtil.isEmpty(attachmentContents)) {
            return null;
        }
        String attachment = attachmentContents.entrySet().stream()
                .map(entry -> "<Attachment name=\"" + entry.getKey() + "\">" + entry.getValue() + "</Attachment>")
                .collect(Collectors.joining("\n\n"));
        return new AiMessage().setRole(AiMessageRoleEnum.USER)
                .setContent(String.format(ATTACHMENT_USER_MESSAGE_TEMPLATE, attachment));
    }

}
