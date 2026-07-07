package com.laby.module.ai.framework.document;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocument;
import com.laby.module.ai.core.document.AiStructuredDocumentElement;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.core.document.AiStructuredDocumentElementTypeEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件解析（EML 简单头解析；MSG 降级为纯文本）
 */
public class EmailDocumentParseClient implements DocumentParseClient {

    private static final Pattern SUBJECT_PATTERN = Pattern.compile("^Subject:\\s*(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_PATTERN = Pattern.compile("^From:\\s*(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("^Date:\\s*(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    @Override
    public AiDocumentParseEngineEnum engine() {
        return AiDocumentParseEngineEnum.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiStructuredDocumentParseResult parse(byte[] bytes, String fileName) {
        String raw = new String(bytes, StandardCharsets.UTF_8);
        boolean isEml = fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".eml");
        if (!isEml) {
            return new AiStructuredDocumentParseResult()
                    .setMarkdown(raw.trim())
                    .setEngine(AiDocumentParseEngineEnum.EMAIL)
                    .setQuality(AiDocumentParseQualityEnum.LOW);
        }

        String subject = matchGroup(SUBJECT_PATTERN, raw);
        String from = matchGroup(FROM_PATTERN, raw);
        String date = matchGroup(DATE_PATTERN, raw);
        String body = extractBody(raw);

        List<AiStructuredDocumentElement> elements = new ArrayList<>();
        if (StrUtil.isNotBlank(subject)) {
            elements.add(new AiStructuredDocumentElement().setType(AiStructuredDocumentElementTypeEnum.TITLE.getCode()).setText(subject).setLevel(1).setPage(1));
        }
        if (StrUtil.isNotBlank(from)) {
            elements.add(new AiStructuredDocumentElement().setType(AiStructuredDocumentElementTypeEnum.TEXT.getCode()).setText("发件人: " + from).setPage(1));
        }
        if (StrUtil.isNotBlank(date)) {
            elements.add(new AiStructuredDocumentElement().setType(AiStructuredDocumentElementTypeEnum.TEXT.getCode()).setText("日期: " + date).setPage(1));
        }
        if (StrUtil.isNotBlank(body)) {
            elements.add(new AiStructuredDocumentElement().setType(AiStructuredDocumentElementTypeEnum.TEXT.getCode()).setText(body).setPage(1));
        }

        StringBuilder markdown = new StringBuilder();
        if (StrUtil.isNotBlank(subject)) {
            markdown.append("# ").append(subject).append("\n\n");
        }
        if (StrUtil.isNotBlank(from)) {
            markdown.append("**发件人:** ").append(from).append("\n\n");
        }
        if (StrUtil.isNotBlank(date)) {
            markdown.append("**日期:** ").append(date).append("\n\n");
        }
        if (StrUtil.isNotBlank(body)) {
            markdown.append(body.trim());
        }

        return new AiStructuredDocumentParseResult()
                .setMarkdown(markdown.toString().trim())
                .setEngine(AiDocumentParseEngineEnum.EMAIL)
                .setQuality(AiDocumentParseQualityEnum.STANDARD)
                .setStructuredDocument(new AiStructuredDocument().setElements(elements));
    }

    private static String matchGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String extractBody(String raw) {
        int splitIndex = raw.indexOf("\r\n\r\n");
        if (splitIndex < 0) {
            splitIndex = raw.indexOf("\n\n");
        }
        if (splitIndex < 0) {
            return raw.trim();
        }
        return raw.substring(splitIndex).trim();
    }

}
