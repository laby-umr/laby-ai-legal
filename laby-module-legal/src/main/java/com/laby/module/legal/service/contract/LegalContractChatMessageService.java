package com.laby.module.legal.service.contract;

import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatMessageRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;

import java.util.List;

/**
 * 法务合同问答消息 Service
 */
public interface LegalContractChatMessageService {

    /**
     * 查询当前用户在指定合同下的全部问答消息
     */
    List<LegalContractChatMessageRespVO> listMessages(Long contractId, Long userId);

    List<LegalContractChatMessageRespVO> listMessages(Long contractId, Long userId, String sessionId);

    /**
     * 构建 Prompt 用的历史消息（不含本轮）
     */
    List<LegalContractChatMessageDO> listHistoryBefore(Long contractId, Long userId, Long beforeMessageId);

    List<LegalContractChatMessageDO> listHistoryBefore(Long contractId, Long userId, String sessionId,
                                                       Long beforeMessageId);

    /**
     * 流式问答开始前写入 user + assistant 占位消息
     */
    StreamTurn beginStreamTurn(Long contractId, Long userId, String message, boolean agentMode, String sessionId);

    /**
     * 流式结束后更新 assistant 消息
     */
    void finalizeAssistantMessage(Long assistantMessageId, String content, String reasoningContent);

    /**
     * 流式失败且无内容时删除占位 assistant
     */
    void deleteAssistantIfEmpty(Long assistantMessageId);

    /**
     * 删除单条消息（user 会连带删除其 assistant）
     */
    void deleteMessage(Long id, Long userId);

    /**
     * 从指定消息起删除后续消息（用于重新生成）
     */
    void deleteFromMessage(Long id, Long userId);

    /**
     * 清空指定合同下当前用户的全部问答
     */
    void clearMessages(Long contractId, Long userId);

    /**
     * 校验消息归属
     */
    LegalContractChatMessageDO validateMessageExists(Long id, Long userId);

    record StreamTurn(LegalContractChatMessageDO userMessage, LegalContractChatMessageDO assistantMessage) {
    }

}
