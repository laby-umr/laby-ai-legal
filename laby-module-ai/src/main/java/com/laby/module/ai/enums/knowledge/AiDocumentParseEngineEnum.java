package com.laby.module.ai.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 知识库文档解析引擎
 */
@AllArgsConstructor
@Getter
public enum AiDocumentParseEngineEnum {

    AUTO("auto", "自动路由"),
    MINERU("mineru", "MinerU 布局解析"),
    DOCLING("docling", "Docling 结构化解析"),
    HTML("html", "HTML 结构化解析"),
    SPREADSHEET("spreadsheet", "电子表格解析"),
    EMAIL("email", "邮件解析"),
    TIKA("tika", "Apache Tika 纯文本");

    private final String code;
    private final String name;

    public static AiDocumentParseEngineEnum valueOfCode(String code) {
        if (code == null) {
            return TIKA;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(TIKA);
    }

}
