package com.laby.module.legal.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 法务编排 Tool 上下文 Redis 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "laby.legal.orchestration")
public class LegalOrchestrationProperties {

    /** Tool 上下文 Redis TTL（小时） */
    private int toolContextTtlHours = 24;

}
