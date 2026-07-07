package com.laby.module.ai.framework.agentscope.config;

import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;

/**
 * HarnessAgent 构建器公共选项（Compaction、maxIters 等）。
 */
public final class AgentScopeHarnessBuilderSupport {

    private AgentScopeHarnessBuilderSupport() {
    }

    public static HarnessAgent.Builder applyCommonOptions(HarnessAgent.Builder builder,
                                                          AgentScopeProperties properties) {
        if (properties.getDefaultMaxSteps() > 0) {
            builder.maxIters(properties.getDefaultMaxSteps());
        }
        if (properties.getCompactionTokenThreshold() > 0) {
            builder.compaction(CompactionConfig.builder()
                    .triggerTokens(properties.getCompactionTokenThreshold())
                    .build());
        }
        return builder;
    }

}
