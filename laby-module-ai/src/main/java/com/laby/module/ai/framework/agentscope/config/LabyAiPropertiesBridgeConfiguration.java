package com.laby.module.ai.framework.agentscope.config;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.framework.agentscope.rag.QdrantVectorStoreProperties;
import com.laby.module.ai.framework.ai.rerank.DashScopeRerankProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * 读取 {@code laby.ai.*} 配置，并兼容 legacy {@code spring.ai.*} 键。
 */
@Configuration
public class LabyAiPropertiesBridgeConfiguration {

    @Bean
    @Primary
    public QdrantVectorStoreProperties qdrantVectorStoreProperties(Environment environment) {
        QdrantVectorStoreProperties laby = bindQdrant(environment, "laby.ai.vector-store.qdrant");
        if (environment.containsProperty("laby.ai.vector-store.qdrant.host")) {
            return laby;
        }
        if (environment.containsProperty("spring.ai.vectorstore.qdrant.host")) {
            return bindQdrant(environment, "spring.ai.vectorstore.qdrant");
        }
        return laby;
    }

    @Bean
    @Primary
    public DashScopeRerankProperties dashScopeRerankProperties(Environment environment) {
        DashScopeRerankProperties properties = new DashScopeRerankProperties();
        bindDashScope(environment, "laby.ai.dashscope", properties);
        if (StrUtil.isBlank(properties.getApiKey())
                && environment.containsProperty("spring.ai.dashscope.api-key")) {
            bindDashScope(environment, "spring.ai.dashscope", properties);
        }
        return properties;
    }

    private static QdrantVectorStoreProperties bindQdrant(Environment env, String prefix) {
        QdrantVectorStoreProperties p = new QdrantVectorStoreProperties();
        p.setHost(env.getProperty(prefix + ".host", "127.0.0.1"));
        p.setPort(env.getProperty(prefix + ".port", Integer.class, 6334));
        p.setUseTls(env.getProperty(prefix + ".use-tls", Boolean.class, false));
        p.setApiKey(env.getProperty(prefix + ".api-key"));
        p.setCollectionName(env.getProperty(prefix + ".collection-name", "knowledge_segment"));
        p.setInitializeSchema(env.getProperty(prefix + ".initialize-schema", Boolean.class, false));
        p.setContentFieldName(env.getProperty(prefix + ".content-field-name", "doc_content"));
        return p;
    }

    private static void bindDashScope(Environment env, String prefix, DashScopeRerankProperties p) {
        p.setApiKey(env.getProperty(prefix + ".api-key"));
        String rerankModel = env.getProperty(prefix + ".rerank-model");
        if (StrUtil.isNotBlank(rerankModel)) {
            p.setRerankModel(rerankModel);
        }
    }

}
