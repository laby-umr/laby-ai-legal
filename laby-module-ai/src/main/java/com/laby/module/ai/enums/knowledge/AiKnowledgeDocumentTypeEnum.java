package com.laby.module.ai.enums.knowledge;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 知识库文档类型（扩展名 + 解析结果推断）
 */
@AllArgsConstructor
@Getter
public enum AiKnowledgeDocumentTypeEnum {

    PDF("pdf", "PDF", Set.of("pdf")),
    WORD("word", "Word 文档", Set.of("docx")),
    WORD_LEGACY("word_legacy", "Word 旧版", Set.of("doc")),
    PRESENTATION("presentation", "演示文稿", Set.of("ppt", "pptx")),
    SPREADSHEET("spreadsheet", "电子表格", Set.of("xls", "xlsx")),
    CSV("csv", "CSV", Set.of("csv")),
    HTML("html", "HTML", Set.of("html", "htm")),
    MARKDOWN("markdown", "Markdown", Set.of("md", "mdx", "markdown")),
    PLAIN_TEXT("plain_text", "纯文本", Set.of("txt")),
    XML("xml", "XML", Set.of("xml")),
    EPUB("epub", "EPUB", Set.of("epub")),
    EMAIL("email", "邮件", Set.of("eml", "msg")),
    UNKNOWN("unknown", "未知", Set.of());

    private final String code;
    private final String name;
    private final Set<String> extensions;

    public static AiKnowledgeDocumentTypeEnum valueOfCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public static AiKnowledgeDocumentTypeEnum fromFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return UNKNOWN;
        }
        String ext = FileNameUtil.extName(fileName);
        if (StrUtil.isBlank(ext)) {
            return UNKNOWN;
        }
        String normalized = ext.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.extensions.contains(normalized))
                .findFirst()
                .orElse(UNKNOWN);
    }

}
