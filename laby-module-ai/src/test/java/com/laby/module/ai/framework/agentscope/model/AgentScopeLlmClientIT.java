package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.core.llm.*;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("需要 DASHSCOPE_API_KEY 或 OPENAI_API_KEY")
class AgentScopeLlmClientIT {

    @Test
    void call_shouldReturnText() {
        AgentScopeModelConfig config = AgentScopeModelConfig.builder()
                .platform(AiPlatformEnum.TONG_YI)
                .modelName("qwen-plus")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .build();
        AgentScopeLlmClient client = new AgentScopeLlmClient(config, Paths.get(System.getProperty("java.io.tmpdir")));
        AiLlmRequest request = new AiLlmRequest()
                .setMessages(List.of(
                        new AiMessage().setRole(AiMessageRoleEnum.USER).setContent("回复 OK 两个字母")));
        String text = client.call(request);
        assertNotNull(text);
        assertFalse(text.isBlank());
    }
}
