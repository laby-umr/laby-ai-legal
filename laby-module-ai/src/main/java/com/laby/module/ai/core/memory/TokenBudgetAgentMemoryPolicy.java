package com.laby.module.ai.core.memory;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.ai.core.token.AiTokenCounter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 基于 token 预算的历史裁剪（从最新消息向前保留）。
 */
@Component
public class TokenBudgetAgentMemoryPolicy implements AgentMemoryPolicy {

    @Override
    public <T> List<T> trimTail(List<T> items, int tokenBudget, int maxItems,
                                Function<T, String> textExtractor, Function<T, Integer> tokenEstimator) {
        if (CollUtil.isEmpty(items)) {
            return List.of();
        }
        List<T> working = new ArrayList<>(items);
        if (maxItems > 0 && working.size() > maxItems) {
            working = working.subList(working.size() - maxItems, working.size());
        }
        if (tokenBudget <= 0) {
            return working;
        }
        int totalTokens = 0;
        List<T> kept = new ArrayList<>();
        for (int i = working.size() - 1; i >= 0; i--) {
            T item = working.get(i);
            String text = textExtractor.apply(item);
            int tokens = tokenEstimator != null ? tokenEstimator.apply(item)
                    : AiTokenCounter.estimate(text);
            if (!kept.isEmpty() && totalTokens + tokens > tokenBudget) {
                break;
            }
            kept.add(item);
            totalTokens += tokens;
        }
        Collections.reverse(kept);
        return kept;
    }

}
