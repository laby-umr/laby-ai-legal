package com.laby.module.legal.service.contract;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatMessageRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractChatMessageMapper;
import com.laby.module.legal.service.memory.LegalContractMemoryFactExtractor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_CHAT_MESSAGE_NOT_EXISTS;

@Service
public class LegalContractChatMessageServiceImpl implements LegalContractChatMessageService {

    private static final String TYPE_USER = "user";
    private static final String TYPE_ASSISTANT = "assistant";

    @Resource
    private LegalContractChatMessageMapper chatMessageMapper;

    @Resource
    private LegalContractMemoryFactExtractor memoryFactExtractor;

    @Override
    public List<LegalContractChatMessageRespVO> listMessages(Long contractId, Long userId) {
        return listMessages(contractId, userId, null);
    }

    @Override
    public List<LegalContractChatMessageRespVO> listMessages(Long contractId, Long userId, String sessionId) {
        List<LegalContractChatMessageDO> list = chatMessageMapper
                .selectListByContractIdAndUserIdAndSessionId(contractId, userId, sessionId);
        return BeanUtils.toBean(list, LegalContractChatMessageRespVO.class);
    }

    @Override
    public List<LegalContractChatMessageDO> listHistoryBefore(Long contractId, Long userId, Long beforeMessageId) {
        return listHistoryBefore(contractId, userId, null, beforeMessageId);
    }

    @Override
    public List<LegalContractChatMessageDO> listHistoryBefore(Long contractId, Long userId, String sessionId,
                                                              Long beforeMessageId) {
        return chatMessageMapper.selectListBeforeId(contractId, userId, sessionId, beforeMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StreamTurn beginStreamTurn(Long contractId, Long userId, String message, boolean agentMode,
                                      String sessionId) {
        LegalContractChatMessageDO userMessage = LegalContractChatMessageDO.builder()
                .contractId(contractId)
                .userId(userId)
                .type(TYPE_USER)
                .content(StrUtil.trim(message))
                .agentMode(agentMode)
                .sessionId(sessionId)
                .build();
        chatMessageMapper.insert(userMessage);

        LegalContractChatMessageDO assistantMessage = LegalContractChatMessageDO.builder()
                .contractId(contractId)
                .userId(userId)
                .replyId(userMessage.getId())
                .type(TYPE_ASSISTANT)
                .content("")
                .agentMode(agentMode)
                .sessionId(sessionId)
                .build();
        chatMessageMapper.insert(assistantMessage);
        return new StreamTurn(userMessage, assistantMessage);
    }

    @Override
    public void finalizeAssistantMessage(Long assistantMessageId, String content, String reasoningContent) {
        chatMessageMapper.updateById(new LegalContractChatMessageDO()
                .setId(assistantMessageId)
                .setContent(StrUtil.nullToDefault(content, ""))
                .setReasoningContent(StrUtil.blankToDefault(reasoningContent, null)));
        triggerFactExtraction(assistantMessageId);
    }

    private void triggerFactExtraction(Long assistantMessageId) {
        LegalContractChatMessageDO assistant = chatMessageMapper.selectById(assistantMessageId);
        if (assistant == null || assistant.getReplyId() == null) {
            return;
        }
        LegalContractChatMessageDO user = chatMessageMapper.selectById(assistant.getReplyId());
        memoryFactExtractor.extractFactAsync(user, assistant);
    }

    @Override
    public void deleteAssistantIfEmpty(Long assistantMessageId) {
        LegalContractChatMessageDO message = chatMessageMapper.selectById(assistantMessageId);
        if (message == null || StrUtil.isNotBlank(message.getContent())) {
            return;
        }
        chatMessageMapper.deleteById(assistantMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long id, Long userId) {
        LegalContractChatMessageDO message = validateMessageExists(id, userId);
        if (TYPE_USER.equals(message.getType())) {
            LegalContractChatMessageDO assistant = chatMessageMapper.selectByReplyId(message.getId());
            if (assistant != null) {
                chatMessageMapper.deleteById(assistant.getId());
            }
        }
        chatMessageMapper.deleteById(message.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFromMessage(Long id, Long userId) {
        LegalContractChatMessageDO message = validateMessageExists(id, userId);
        chatMessageMapper.deleteFromId(message.getContractId(), userId, message.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearMessages(Long contractId, Long userId) {
        chatMessageMapper.deleteByContractIdAndUserId(contractId, userId);
    }

    @Override
    public LegalContractChatMessageDO validateMessageExists(Long id, Long userId) {
        LegalContractChatMessageDO message = chatMessageMapper.selectById(id);
        if (message == null || ObjUtil.notEqual(message.getUserId(), userId)) {
            throw exception(CONTRACT_CHAT_MESSAGE_NOT_EXISTS);
        }
        return message;
    }

}
