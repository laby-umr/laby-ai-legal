package com.laby.module.ai.framework.ai.config;

import com.laby.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;
import com.laby.module.ai.framework.ai.core.model.suno.api.SunoApi;
import com.laby.module.ai.framework.ai.core.webserch.AiWebSearchClient;
import com.laby.module.ai.framework.ai.core.webserch.bocha.AiBoChaWebSearchClient;
import com.laby.module.ai.framework.ai.rerank.DashScopeRerankClient;
import com.laby.module.ai.framework.ai.rerank.DashScopeRerankProperties;
import com.laby.module.ai.framework.document.DocumentParseAutoConfiguration;
import com.laby.module.ai.framework.knowledge.retrieval.KnowledgeRetrievalAutoConfiguration;
import com.laby.module.ai.framework.knowledge.retrieval.KnowledgeRetrievalProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 芋道 AI 自动配置
 *
 * @author fansili
 */
@Configuration
@EnableConfigurationProperties({LabyAiProperties.class, KnowledgeRetrievalProperties.class})
@Import({DocumentParseAutoConfiguration.class, KnowledgeRetrievalAutoConfiguration.class})
public class AiAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "laby.ai.midjourney.enable", havingValue = "true")
    public MidjourneyApi midjourneyApi(LabyAiProperties labyAiProperties) {
        LabyAiProperties.Midjourney config = labyAiProperties.getMidjourney();
        return new MidjourneyApi(config.getBaseUrl(), config.getApiKey(), config.getNotifyUrl());
    }

    @Bean
    @ConditionalOnProperty(value = "laby.ai.suno.enable", havingValue = "true")
    public SunoApi sunoApi(LabyAiProperties labyAiProperties) {
        return new SunoApi(labyAiProperties.getSuno().getBaseUrl());
    }

    @Bean
    @ConditionalOnProperty(value = "laby.ai.web-search.enable", havingValue = "true")
    public AiWebSearchClient webSearchClient(LabyAiProperties labyAiProperties) {
        return new AiBoChaWebSearchClient(labyAiProperties.getWebSearch().getApiKey());
    }

    @Bean
    @ConditionalOnExpression("'${laby.ai.model.rerank:false}' == 'true' or '${spring.ai.model.rerank:false}' == 'true'")
    public DashScopeRerankClient dashScopeRerankClient(DashScopeRerankProperties properties) {
        return new DashScopeRerankClient(properties);
    }

}
