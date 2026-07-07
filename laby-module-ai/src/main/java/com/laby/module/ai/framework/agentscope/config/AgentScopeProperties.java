package com.laby.module.ai.framework.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "laby.ai.agentscope")
public class AgentScopeProperties {

    /** Harness 工作区根目录 */
    private String workspacePath = "${java.io.tmpdir}/laby-agentscope";
    private String sessionKeyPrefix = "as:";

    /** Session 存储：workspace（WorkspaceSession）| memory（InMemorySession）| redis（RedisBackedJsonSession） */
    private String sessionStore = "workspace";

    /** Redis Session TTL（小时），仅 session-store=redis 时生效 */
    private int sessionTtlHours = 24;

    /** 模型调用失败后的最大重试次数（不含首次请求） */
    private int modelMaxRetries = 2;

    /** Agent ReAct 最大步数，0 表示使用 Harness 默认 */
    private int defaultMaxSteps = 12;

    /** 上下文 Compaction 触发 token 阈值，0 表示关闭 */
    private int compactionTokenThreshold = 120_000;

    /** 纯 LLM 路径注入 DB 历史的 token 预算 */
    private int historyTokenBudget = 32_000;

    /** Compaction 摘要是否写入 ai_chat_message（type=summary） */
    private boolean compactionSummaryPersist = false;

    /** 审核 Orchestrator Sub-agent 试点（仅记录规划日志，不改变批审结果） */
    private boolean auditSubagentPilot = false;

}
