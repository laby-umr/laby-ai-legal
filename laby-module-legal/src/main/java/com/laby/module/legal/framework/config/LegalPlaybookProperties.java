package com.laby.module.legal.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 法务 Playbook 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "laby.legal.playbook")
public class LegalPlaybookProperties {

    /**
     * 是否在 AI 审核前运行确定性 Playbook
     */
    private Boolean enabled = true;

}
