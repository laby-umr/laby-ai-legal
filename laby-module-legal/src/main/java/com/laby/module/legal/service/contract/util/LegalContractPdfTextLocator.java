package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.Locale;

/**
 * PDF 文本锚点定位（用于批注落点）。
 */
public final class LegalContractPdfTextLocator {

    private static final int MIN_ANCHOR_LEN = 6;

    private LegalContractPdfTextLocator() {
    }

    public static TextAnchor locate(byte[] pdfBytes, String anchorText) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0 || StrUtil.isBlank(anchorText)) {
            return null;
        }
        String needle = normalize(anchorText);
        if (needle.length() < MIN_ANCHOR_LEN) {
            needle = normalize(StrUtil.sub(anchorText, 0, Math.min(anchorText.length(), 40)));
        }
        if (StrUtil.isBlank(needle)) {
            return null;
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                stripper.setStartPage(pageIndex + 1);
                stripper.setEndPage(pageIndex + 1);
                String pageText = stripper.getText(document);
                if (containsNormalized(pageText, needle)) {
                    PDPage page = document.getPage(pageIndex);
                    PDRectangle box = page.getMediaBox();
                    float x = box.getWidth() - 28;
                    float y = box.getHeight() - 72;
                    return new TextAnchor(pageIndex, x, y);
                }
            }
            stripper.setStartPage(1);
            stripper.setEndPage(pageCount);
            if (containsNormalized(stripper.getText(document), needle)) {
                PDPage page = document.getPage(0);
                PDRectangle box = page.getMediaBox();
                return new TextAnchor(0, box.getWidth() - 28, box.getHeight() - 72);
            }
        }
        return null;
    }

    public static String pickAnchorText(String oldText, String paragraphText) {
        if (StrUtil.isNotBlank(oldText)) {
            return oldText;
        }
        if (StrUtil.isNotBlank(paragraphText)) {
            return StrUtil.sub(paragraphText, 0, Math.min(paragraphText.length(), 80));
        }
        return null;
    }

    private static boolean containsNormalized(String haystack, String needle) {
        if (StrUtil.isBlank(haystack) || StrUtil.isBlank(needle)) {
            return false;
        }
        String normalizedHaystack = normalize(haystack);
        String normalizedNeedle = normalize(needle);
        return normalizedHaystack.contains(normalizedNeedle)
                || normalizedNeedle.contains(normalizedHaystack) && normalizedHaystack.length() >= MIN_ANCHOR_LEN;
    }

    private static String normalize(String text) {
        return StrUtil.trim(text)
                .replaceAll("[\\s\u00a0\u200b\u3000]+", "")
                .toLowerCase(Locale.ROOT);
    }

    @Data
    @AllArgsConstructor
    public static class TextAnchor {
        private int pageIndex;
        private float x;
        private float y;
    }

}
