package com.laby.module.legal.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 法务审核并发与队列配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "laby.legal.audit")
public class LegalAuditProperties {

    /** 单租户同时执行的 AI 审核上限 */
    private int maxConcurrentPerTenant = 5;

}
