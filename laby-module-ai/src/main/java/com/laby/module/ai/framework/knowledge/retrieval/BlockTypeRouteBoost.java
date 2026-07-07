package com.laby.module.ai.framework.knowledge.retrieval;

import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;

/**
 * 按查询意图对块类型分数加权
 */
public final class BlockTypeRouteBoost {

    private BlockTypeRouteBoost() {
    }

    public static double apply(AiQueryIntentEnum intent, String blockType, double score,
                               KnowledgeRetrievalProperties.BlockRouteConfig config) {
        if (intent == null || score <= 0 || config == null || !config.isEnabled()) {
            return score;
        }
        AiKnowledgeSegmentBlockTypeEnum block = AiKnowledgeSegmentBlockTypeEnum.valueOfCode(blockType);
        return switch (intent) {
            case TABLE_CELL -> switch (block) {
                case TABLE_ROW -> score * config.getTableCellBoost();
                case TABLE_WHOLE -> score * config.getTableWholeBoost();
                default -> score;
            };
            case TABLE_OVERVIEW -> block == AiKnowledgeSegmentBlockTypeEnum.TABLE_SUMMARY
                    ? score * config.getTableSummaryBoost()
                    : score;
            default -> score;
        };
    }

}
