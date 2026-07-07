package com.laby.module.ai.framework.agentscope.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.framework.agentscope.chat.middleware.AiChatCompactionSummaryMiddleware;
import com.laby.module.ai.framework.agentscope.chat.middleware.AiChatTraceMiddleware;
import com.laby.module.ai.framework.agentscope.config.AgentScopeHarnessBuilderSupport;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelFactory;
import com.laby.module.ai.framework.agentscope.session.AgentScopeSessionFactory;
import com.laby.module.ai.framework.agentscope.session.AgentScopeSessionKeyBuilder;
import com.laby.module.ai.service.chat.AiChatCompactionSummaryService;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天 HarnessAgent 构建器。
 */
@Component
public class AiChatAgentScopeConfig {

    private static final String DEFAULT_SYS_PROMPT = "You are a helpful assistant.";

    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private AgentScopeSessionFactory agentScopeSessionFactory;
    @Resource
    private AiChatCompactionSummaryService compactionSummaryService;

    public HarnessAgent buildAgent(AiModelDO model, AiApiKeyDO apiKey, Toolkit toolkit,
                                   String sysPrompt, String sessionId) {
        return buildAgent(model, apiKey, toolkit, sysPrompt, sessionId, null);
    }

    public HarnessAgent buildAgent(AiModelDO model, AiApiKeyDO apiKey, Toolkit toolkit,
                                   String sysPrompt, String sessionId, List<MiddlewareBase> extraMiddlewares) {
        return buildAgent(model, apiKey, toolkit, sysPrompt, sessionId, extraMiddlewares, null);
    }

    public HarnessAgent buildAgent(AiModelDO model, AiApiKeyDO apiKey, Toolkit toolkit,
                                   String sysPrompt, String sessionId, List<MiddlewareBase> extraMiddlewares,
                                   ToolExecutionContext toolExecutionContext) {
        List<MiddlewareBase> middlewares = new ArrayList<>();
        middlewares.add(new AiChatTraceMiddleware());
        if (agentScopeProperties.isCompactionSummaryPersist()) {
            middlewares.add(new AiChatCompactionSummaryMiddleware(compactionSummaryService));
        }
        if (CollUtil.isNotEmpty(extraMiddlewares)) {
            middlewares.addAll(extraMiddlewares);
        }

        Path workspace = Paths.get(agentScopeProperties.getWorkspacePath(), "chat", sessionId);
        Session session = agentScopeSessionFactory.createSession(workspace);
        SessionKey sessionKey = AgentScopeSessionKeyBuilder.aiChat(
                agentScopeProperties.getSessionKeyPrefix(), sessionId);

        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name("ai-chat-agent")
                .sysPrompt(StrUtil.isNotBlank(sysPrompt) ? sysPrompt : DEFAULT_SYS_PROMPT)
                .model(AgentScopeModelFactory.buildChatModel(AgentScopeModelFactory.from(model, apiKey),
                        agentScopeProperties.getModelMaxRetries()))
                .toolkit(toolkit)
                .middlewares(middlewares);
        if (toolExecutionContext != null) {
            builder.toolExecutionContext(toolExecutionContext);
        }
        builder.session(session)
                .sessionKey(sessionKey)
                .disableSubagents()
                .disableDynamicSkills()
                .workspace(workspace);
        AgentScopeHarnessBuilderSupport.applyCommonOptions(builder, agentScopeProperties);
        return builder.build();
    }

}
