package com.laby.module.ai.framework.agentscope.chat;

import com.laby.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Compaction 摘要落库所需的对话元数据（经 {@link io.agentscope.core.agent.RuntimeContext} 传递）。
 */
@Data
@Accessors(chain = true)
public class AiChatCompactionSummaryContext {

    private Long conversationId;
    private Long userId;
    private Long roleId;
    private Long modelId;
    private String model;

    public static AiChatCompactionSummaryContext of(AiChatConversationDO conversation, AiModelDO model) {
        return new AiChatCompactionSummaryContext()
                .setConversationId(conversation.getId())
                .setUserId(conversation.getUserId())
                .setRoleId(conversation.getRoleId())
                .setModelId(model.getId())
                .setModel(model.getModel());
    }

}
