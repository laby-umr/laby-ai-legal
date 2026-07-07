package com.laby.module.ai.framework.document;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocument;
import com.laby.module.ai.core.document.AiStructuredDocumentElement;
import com.laby.module.ai.core.document.AiStructuredDocumentElementTypeEnum;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HTML 结构化解析（Jsoup）
 */
public class HtmlStructuredDocumentParseClient implements DocumentParseClient {

    @Override
    public AiDocumentParseEngineEnum engine() {
        return AiDocumentParseEngineEnum.HTML;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiStructuredDocumentParseResult parse(byte[] bytes, String fileName) {
        String html = new String(bytes, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(html);
        List<AiStructuredDocumentElement> elements = new ArrayList<>();
        StringBuilder markdown = new StringBuilder();

        for (Element heading : doc.select("h1, h2, h3, h4, h5, h6")) {
            String text = heading.text().trim();
            if (StrUtil.isBlank(text)) {
                continue;
            }
            int level = headingTagLevel(heading.tagName());
            elements.add(new AiStructuredDocumentElement()
                    .setType(AiStructuredDocumentElementTypeEnum.TITLE.getCode())
                    .setText(text)
                    .setLevel(level)
                    .setPage(1));
            markdown.append("#".repeat(Math.max(1, level))).append(" ").append(text).append("\n\n");
        }

        for (Element table : doc.select("table")) {
            String tableMarkdown = tableToMarkdown(table);
            if (StrUtil.isBlank(tableMarkdown)) {
                continue;
            }
            String caption = StrUtil.blankToDefault(table.attr("summary"), "表格");
            elements.add(new AiStructuredDocumentElement()
                    .setType(AiStructuredDocumentElementTypeEnum.TABLE.getCode())
                    .setMarkdown(tableMarkdown)
                    .setCaption(caption)
                    .setPage(1));
            markdown.append("### ").append(caption).append("\n").append(tableMarkdown).append("\n\n");
        }

        for (Element paragraph : doc.select("p, li, blockquote")) {
            String text = paragraph.text().trim();
            if (StrUtil.isBlank(text)) {
                continue;
            }
            elements.add(new AiStructuredDocumentElement()
                    .setType(AiStructuredDocumentElementTypeEnum.TEXT.getCode())
                    .setText(text)
                    .setPage(1));
            markdown.append(text).append("\n\n");
        }

        if (elements.isEmpty()) {
            String bodyText = doc.body() != null ? doc.body().text().trim() : doc.text().trim();
            if (StrUtil.isNotBlank(bodyText)) {
                elements.add(new AiStructuredDocumentElement().setType(AiStructuredDocumentElementTypeEnum.TEXT.getCode()).setText(bodyText).setPage(1));
                markdown.append(bodyText);
            }
        }

        return new AiStructuredDocumentParseResult()
                .setMarkdown(markdown.toString().trim())
                .setEngine(AiDocumentParseEngineEnum.HTML)
                .setQuality(AiDocumentParseQualityEnum.STANDARD)
                .setStructuredDocument(new AiStructuredDocument().setElements(elements));
    }

    private static int headingTagLevel(String tagName) {
        if (tagName == null || tagName.length() != 2 || !tagName.startsWith("h")) {
            return 2;
        }
        try {
            return Integer.parseInt(tagName.substring(1));
        } catch (NumberFormatException ex) {
            return 2;
        }
    }

    private static String tableToMarkdown(Element table) {
        Elements rows = table.select("tr");
        if (rows.isEmpty()) {
            return "";
        }
        List<List<String>> cells = new ArrayList<>();
        for (Element row : rows) {
            Elements cols = row.select("th, td");
            if (cols.isEmpty()) {
                continue;
            }
            List<String> rowCells = new ArrayList<>();
            for (Element col : cols) {
                rowCells.add(col.text().trim());
            }
            cells.add(rowCells);
        }
        if (cells.isEmpty()) {
            return "";
        }
        int colCount = cells.stream().mapToInt(List::size).max().orElse(0);
        if (colCount == 0) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        List<String> header = padRow(cells.get(0), colCount);
        lines.add("| " + String.join(" | ", header) + " |");
        lines.add("| " + String.join(" | ", Collections.nCopies(colCount, "---")) + " |");
        for (int i = 1; i < cells.size(); i++) {
            lines.add("| " + String.join(" | ", padRow(cells.get(i), colCount)) + " |");
        }
        return String.join("\n", lines);
    }

    private static List<String> padRow(List<String> row, int colCount) {
        List<String> padded = new ArrayList<>(row);
        while (padded.size() < colCount) {
            padded.add("");
        }
        return padded;
    }

}
