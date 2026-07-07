package com.laby.module.ai.core.memory;

import java.util.List;
import java.util.function.Function;

/**
 * Agent 历史消息裁剪策略（按 token 预算与条数上限）。
 */
public interface AgentMemoryPolicy {

    /**
     * 从尾部保留尽可能多的条目，使累计 token 不超过 budget。
     *
     * @param items           按时间正序的历史条目
     * @param tokenBudget     token 上限，&lt;= 0 表示不限制
     * @param maxItems        条数上限，&lt;= 0 表示不限制
     * @param tokenEstimator  单条 token 估算
     * @param <T>             条目类型
     * @return 裁剪后的子列表（保持原顺序）
     */
    <T> List<T> trimTail(List<T> items, int tokenBudget, int maxItems,
                         Function<T, String> textExtractor, Function<T, Integer> tokenEstimator);

}
