package com.laby.module.ai.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 知识库文档解析质量等级
 */
@AllArgsConstructor
@Getter
public enum AiDocumentParseQualityEnum {

    HIGH("high", "高质量（布局 + 表格结构）"),
    STANDARD("standard", "标准（有结构无 OCR）"),
    LOW("low", "降级（纯文本）");

    private final String code;
    private final String name;

    public static AiDocumentParseQualityEnum valueOfCode(String code) {
        if (code == null) {
            return LOW;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(LOW);
    }

}
