package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 全文检索用纯文本归一化（去 Markdown / 表格符号，保留中英数字）
 */
public final class SparseTextNormalizer {

    private static final Pattern MARKDOWN_NOISE = Pattern.compile("[#*_>`\\[\\](){}|~\\-]+");
    private static final Pattern NON_SEARCHABLE = Pattern.compile("[^\\p{IsHan}\\p{L}\\p{N}\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SparseTextNormalizer() {
    }

    /**
     * 将 embed/content 文本转为可供 FULLTEXT / BM25 检索的纯文本
     */
    public static String stripMarkdown(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String normalized = text.replace('|', ' ');
        normalized = MARKDOWN_NOISE.matcher(normalized).replaceAll(" ");
        normalized = NON_SEARCHABLE.matcher(normalized).replaceAll(" ");
        return WHITESPACE.matcher(normalized.trim()).replaceAll(" ");
    }

    /** 查询与入库共用归一化 */
    public static String normalize(String text) {
        return stripMarkdown(text);
    }

}
