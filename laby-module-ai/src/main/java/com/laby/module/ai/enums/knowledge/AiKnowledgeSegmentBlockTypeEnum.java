package com.laby.module.ai.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 知识库分段块类型（结构化 RAG）
 */
@AllArgsConstructor
@Getter
public enum AiKnowledgeSegmentBlockTypeEnum {

    TEXT("text", "正文", true),
    TITLE("title", "标题", true),
    TABLE_WHOLE("table_whole", "整表", true),
    TABLE_ROW("table_row", "表格行", true),
    TABLE_SUMMARY("table_summary", "表格摘要", true),
    IMAGE("image", "图片说明", true),
    FORMULA("formula", "公式", true),
    CODE("code", "代码块", true);

    private final String code;
    private final String name;
    /** 默认是否写入向量库 */
    private final boolean embedByDefault;

    public static AiKnowledgeSegmentBlockTypeEnum valueOfCode(String code) {
        if (code == null) {
            return TEXT;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(TEXT);
    }

}
