package com.laby.module.ai.framework.agentscope.image;

import com.laby.module.ai.core.image.AiImageClient;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelConfig;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelFactory;

public final class AgentScopeImageClientFactory {

    private AgentScopeImageClientFactory() {
    }

    public static AiImageClient build(AiModelDO model, AiApiKeyDO apiKey) {
        AgentScopeModelConfig config = AgentScopeModelFactory.from(model, apiKey);
        return new HttpAiImageClient(config);
    }

    public static AiImageClient build(AgentScopeModelConfig config) {
        return new HttpAiImageClient(config);
    }

}
