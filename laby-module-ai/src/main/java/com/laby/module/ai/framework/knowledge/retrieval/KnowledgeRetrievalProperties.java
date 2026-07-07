package com.laby.module.ai.framework.knowledge.retrieval;

import com.laby.module.ai.enums.knowledge.AiRagNoAnswerPolicyEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Universal RAG 检索配置
 */
@Data
@ConfigurationProperties(prefix = "laby.ai.knowledge-retrieval")
public class KnowledgeRetrievalProperties {

    private boolean enabled = false;

    private int defaultTopK = 8;

    private double defaultSimilarityThreshold = 0.60;

    /** 宽召回倍数 */
    private int retrievalFactor = 4;

    private double minAnswerScore = 0.45;

    private String noAnswerPolicy = AiRagNoAnswerPolicyEnum.STRICT.getCode();

    private HybridConfig hybrid = new HybridConfig();

    private MultiQueryConfig multiQuery = new MultiQueryConfig();

    private RerankConfig rerank = new RerankConfig();

    private BlockRouteConfig blockRoute = new BlockRouteConfig();

    private DiagnosticsConfig diagnostics = new DiagnosticsConfig();

    @Data
    public static class HybridConfig {

        private boolean enabled = true;
        private boolean sparseEnabled = true;
        private int rrfK = 60;
        private double denseWeight = 1.0;
        private double sparseWeight = 1.2;

    }

    @Data
    public static class MultiQueryConfig {

        private boolean enabled = true;
        private int maxVariants = 3;
        /** rule | llm | hybrid */
        private String mode = "rule";
        private Long llmModelId;

    }

    @Data
    public static class RerankConfig {

        private boolean enabled = true;
        private String model = "gte-rerank-v2";

    }

    @Data
    public static class BlockRouteConfig {

        private boolean enabled = true;
        private double tableCellBoost = 1.3;
        private double tableWholeBoost = 1.1;
        private double tableSummaryBoost = 1.4;

    }

    @Data
    public static class DiagnosticsConfig {

        private boolean enabled = true;
        private long logSlowMs = 500;

    }

    public AiRagNoAnswerPolicyEnum resolvedNoAnswerPolicy() {
        return AiRagNoAnswerPolicyEnum.valueOfCode(noAnswerPolicy);
    }

}
