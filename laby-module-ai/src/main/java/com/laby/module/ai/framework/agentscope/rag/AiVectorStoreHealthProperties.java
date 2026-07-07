package com.laby.module.ai.framework.agentscope.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库向量健康检查与自动修复配置。
 */
@Data
@ConfigurationProperties(prefix = "laby.ai.vector-store.health")
public class AiVectorStoreHealthProperties {

    /** 是否启用定时健康检查 */
    private boolean enabled = true;

    /** 检查时是否自动修复缺失/模型不一致的向量 */
    private boolean autoRepair = true;

    /** 单次 retrieve 批量大小 */
    private int batchSize = 100;

    /** 单次定时任务最多修复条数，防止 Embedding API 被打满 */
    private int maxRepairsPerRun = 500;

    /** 定时任务 cron */
    private String cron = "0 30 3 * * ?";

}
