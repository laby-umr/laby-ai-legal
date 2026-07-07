package com.laby.module.ai.core.document;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 结构化文档元素类型（与 Parser HTTP 服务 JSON 字段 type 对齐）
 */
@AllArgsConstructor
@Getter
public enum AiStructuredDocumentElementTypeEnum {

    TITLE("title"),
    TEXT("text"),
    TABLE("table"),
    IMAGE("image"),
    FORMULA("formula");

    private final String code;

    public static AiStructuredDocumentElementTypeEnum valueOfCode(String code) {
        if (code == null) {
            return TEXT;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(TEXT);
    }

}
