package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.enums.model.AiPlatformEnum;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentScopeModelConfig {

    private AiPlatformEnum platform;
    private String modelName;
    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;

}
