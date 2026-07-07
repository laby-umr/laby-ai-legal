package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * docx 段落解析（Apache POI）
 */
public final class LegalContractWordParser {

    private LegalContractWordParser() {
    }

    public static List<ParagraphItem> parseDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            return parseParagraphs(document);
        }
    }

    public static List<ParagraphItem> parseParagraphs(XWPFDocument document) {
        List<ParagraphItem> items = new ArrayList<>();
        int sort = 0;
        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                sort = appendParagraph(items, sort, paragraph);
            } else if (element instanceof XWPFTable table) {
                sort = appendTableParagraphs(items, sort, table);
            }
        }
        return items;
    }

    private static int appendTableParagraphs(List<ParagraphItem> items, int sort, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    sort = appendParagraph(items, sort, paragraph);
                }
            }
        }
        return sort;
    }

    private static int appendParagraph(List<ParagraphItem> items, int sort, XWPFParagraph paragraph) {
        String text = StrUtil.trim(paragraph.getText());
        if (StrUtil.isBlank(text)) {
            return sort;
        }
        sort++;
        items.add(new ParagraphItem("p-" + sort, sort, text, null));
        return sort;
    }

    @Data
    @AllArgsConstructor
    public static class ParagraphItem {
        private String paragraphId;
        private Integer sort;
        private String text;
        private String path;
    }

}
