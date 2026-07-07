package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.SystemMessage;
import io.agentscope.core.message.UserMessage;

import java.util.ArrayList;
import java.util.List;

public final class AiMessageConverter {

    private AiMessageConverter() {}

    public static List<Msg> toAgentScopeMessages(List<AiMessage> messages) {
        List<Msg> result = new ArrayList<>(messages.size());
        for (AiMessage message : messages) {
            result.add(switch (message.getRole()) {
                case SYSTEM -> new SystemMessage(message.getContent());
                case USER -> new UserMessage(message.getContent());
                case ASSISTANT -> new AssistantMessage(message.getContent());
                case TOOL -> new UserMessage("[tool:" + message.getToolName() + "] " + message.getContent());
            });
        }
        return result;
    }

}
