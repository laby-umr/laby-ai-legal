package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;

/**
 * 规则优先的查询意图分类
 */
public final class QueryIntentClassifier {

    private static final String SECTION_PATTERN = "第.*章|条款|slide|Slide|页|章节|幻灯片";
    private static final String TABLE_OVERVIEW_PATTERN = "总结|概述|有哪些|概况|汇总|讲什么";
    private static final String TABLE_CELL_PATTERN =
            "多少|几岁|年龄|哪一行|Age|age|Profit|profit|利润|金额|工资|薪资|单价|数量|有哪些列";
    private static final String ENTITY_PATTERN = "[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*|\\b[A-Z]{2,}\\b|\\d{4,}";

    private QueryIntentClassifier() {
    }

    public static AiQueryIntentEnum classify(String query) {
        if (StrUtil.isBlank(query)) {
            return AiQueryIntentEnum.GENERAL;
        }
        String text = StrUtil.trim(query);
        if (ReUtil.contains(SECTION_PATTERN, text)) {
            return AiQueryIntentEnum.SECTION;
        }
        if (ReUtil.contains(TABLE_OVERVIEW_PATTERN, text)) {
            return AiQueryIntentEnum.TABLE_OVERVIEW;
        }
        if (ReUtil.contains(TABLE_CELL_PATTERN, text)) {
            return AiQueryIntentEnum.TABLE_CELL;
        }
        if (ReUtil.contains(ENTITY_PATTERN, text)) {
            return AiQueryIntentEnum.ENTITY;
        }
        return AiQueryIntentEnum.GENERAL;
    }

}
