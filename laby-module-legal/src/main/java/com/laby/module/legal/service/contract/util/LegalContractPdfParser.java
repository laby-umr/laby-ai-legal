package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.service.contract.util.LegalContractWordParser.ParagraphItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PDF 文字层段落化（与 Word {@link LegalContractWordParser} 输出同构）。
 */
public final class LegalContractPdfParser {

    private static final int MIN_CHARS_PER_PAGE = 30;

    private static final Pattern CLAUSE_HEADING = Pattern.compile(
            "^(第[一二三四五六七八九十百千零〇\\d]+条|[\\d]+[\\.、．])\\s*");

    private LegalContractPdfParser() {
    }

    public static ParseResult parse(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return ParseResult.emptyScan();
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = Math.max(document.getNumberOfPages(), 1);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String fullText = stripper.getText(document);
            if (isLikelyScan(fullText, pageCount)) {
                return ParseResult.emptyScan();
            }
            List<ParagraphItem> items = new ArrayList<>();
            int sort = 0;
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                sort = appendParagraphsFromText(items, sort, pageText);
            }
            if (items.isEmpty() && StrUtil.isNotBlank(fullText)) {
                sort = appendParagraphsFromText(items, sort, fullText);
            }
            return new ParseResult(items, false);
        }
    }

    /**
     * 启发式分段：空行切块 + 条款标题行单独成段。
     */
    public static List<ParagraphItem> splitParagraphs(String text) {
        List<ParagraphItem> items = new ArrayList<>();
        appendParagraphsFromText(items, 0, text);
        return items;
    }

    public static boolean isLikelyScan(String text, int pageCount) {
        if (pageCount <= 0) {
            return true;
        }
        int charCount = StrUtil.trim(text).length();
        return charCount < pageCount * MIN_CHARS_PER_PAGE;
    }

    private static int appendParagraphsFromText(List<ParagraphItem> items, int sort, String rawText) {
        if (StrUtil.isBlank(rawText)) {
            return sort;
        }
        String normalized = rawText.replace('\r', '\n');
        String[] blocks = normalized.split("\n\\s*\n");
        for (String block : blocks) {
            sort = appendBlock(items, sort, block);
        }
        return sort;
    }

    private static int appendBlock(List<ParagraphItem> items, int sort, String block) {
        String trimmed = StrUtil.trim(block);
        if (StrUtil.isBlank(trimmed)) {
            return sort;
        }
        String[] lines = trimmed.split("\n");
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            String lineText = StrUtil.trim(line);
            if (StrUtil.isBlank(lineText)) {
                continue;
            }
            if (CLAUSE_HEADING.matcher(lineText).find() && current.length() > 0) {
                sort = appendOne(items, sort, current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(lineText);
        }
        if (current.length() > 0) {
            sort = appendOne(items, sort, current.toString());
        }
        return sort;
    }

    private static int appendOne(List<ParagraphItem> items, int sort, String text) {
        String paragraphText = StrUtil.trim(text);
        if (StrUtil.isBlank(paragraphText)) {
            return sort;
        }
        sort++;
        items.add(new ParagraphItem("p-" + sort, sort, paragraphText, null));
        return sort;
    }

    @Data
    @AllArgsConstructor
    public static class ParseResult {

        private List<ParagraphItem> paragraphs;

        /** 疑似扫描件 / 无文字层 */
        private boolean scanLikely;

        public static ParseResult emptyScan() {
            return new ParseResult(List.of(), true);
        }

    }

}
