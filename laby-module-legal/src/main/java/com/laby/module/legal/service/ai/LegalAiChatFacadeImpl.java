package com.laby.module.legal.service.ai;

import com.laby.module.ai.service.chat.AiChatMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link LegalAiChatFacade} 实现：委托 AI 模块 Service。
 */
@Service
public class LegalAiChatFacadeImpl implements LegalAiChatFacade {

    @Resource
    private AiChatMessageService aiChatMessageService;

    @Override
    public List<String> listLatestUserAttachmentUrls(Long conversationId) {
        return aiChatMessageService.listLatestUserAttachmentUrls(conversationId);
    }

}
