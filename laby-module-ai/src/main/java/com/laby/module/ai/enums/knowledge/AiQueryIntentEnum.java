package com.laby.module.ai.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * RAG 查询意图（规则分类）
 */
@AllArgsConstructor
@Getter
public enum AiQueryIntentEnum {

    TABLE_CELL("table_cell", "表格单元格/行查询"),
    TABLE_OVERVIEW("table_overview", "表格概览"),
    SECTION("section", "章节查询"),
    ENTITY("entity", "实体查询"),
    GENERAL("general", "通用");

    private final String code;
    private final String name;

    public static AiQueryIntentEnum valueOfCode(String code) {
        if (code == null) {
            return GENERAL;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(GENERAL);
    }

}
