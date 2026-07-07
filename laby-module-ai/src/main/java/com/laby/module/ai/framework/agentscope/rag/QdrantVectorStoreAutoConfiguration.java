package com.laby.module.ai.framework.agentscope.rag;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiVectorStoreHealthProperties.class)
public class QdrantVectorStoreAutoConfiguration {

}

