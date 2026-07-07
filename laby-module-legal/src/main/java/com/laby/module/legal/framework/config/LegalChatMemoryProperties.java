package com.laby.module.legal.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 法务合同问答记忆窗口配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "laby.legal.chat")
public class LegalChatMemoryProperties {

    /** 最大历史轮次（条数兜底） */
    private int maxHistoryTurns = 8;

    /** 注入 LLM 的历史 token 预算 */
    private int historyTokenBudget = 32_000;

    /** 单条消息字符截断上限 */
    private int maxTurnChars = 4_000;

}
