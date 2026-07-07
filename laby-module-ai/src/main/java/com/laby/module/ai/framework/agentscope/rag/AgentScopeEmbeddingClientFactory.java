package com.laby.module.ai.framework.agentscope.rag;

import com.laby.module.ai.core.rag.AiEmbeddingClient;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelConfig;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelFactory;

public final class AgentScopeEmbeddingClientFactory {

    private AgentScopeEmbeddingClientFactory() {
    }

    public static AiEmbeddingClient build(AiModelDO model, AiApiKeyDO apiKey) {
        AgentScopeModelConfig config = AgentScopeModelFactory.from(model, apiKey);
        return new HttpAiEmbeddingClient(config);
    }

    public static AiEmbeddingClient build(AgentScopeModelConfig config) {
        return new HttpAiEmbeddingClient(config);
    }

}
