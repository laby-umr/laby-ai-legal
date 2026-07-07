package com.laby.module.legal.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 法务合同情节记忆配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "laby.legal.memory")
public class LegalContractMemoryProperties {

    /** 是否启用异步事实提取 */
    private boolean factExtractionEnabled = true;

    /** 注入 Agent system 附录的最大条数 */
    private int maxAppendixItems = 10;

    /** 事实提取最小用户消息长度 */
    private int factExtractionMinChars = 20;

    /** 是否使用 LLM 抽取事实（false 时使用启发式拼接） */
    private boolean factExtractionUseLlm = false;

}
