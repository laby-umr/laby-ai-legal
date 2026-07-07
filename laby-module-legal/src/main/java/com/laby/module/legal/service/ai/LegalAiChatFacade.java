package com.laby.module.legal.service.ai;

import java.util.List;

/**
 * Legal 读 AI 对话消息的防腐层（勿直依赖 ai 模块 Mapper/DO）。
 */
public interface LegalAiChatFacade {

    /**
     * 最近一条带附件的用户消息 URL 列表。
     */
    List<String> listLatestUserAttachmentUrls(Long conversationId);

}
