package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 将 Markdown 报告简易转换为 docx（标题/列表/正文，满足归档导出 V1）。
 */
public final class LegalMarkdownDocxConverter {

    private LegalMarkdownDocxConverter() {
    }

    public static byte[] toDocxBytes(String markdown) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<String> lines = StrUtil.split(markdown, '\n');
            for (String line : lines) {
                if (StrUtil.isBlank(line)) {
                    document.createParagraph();
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.startsWith("### ")) {
                    addHeading(document, trimmed.substring(4), 12, true);
                } else if (trimmed.startsWith("## ")) {
                    addHeading(document, trimmed.substring(3), 14, true);
                } else if (trimmed.startsWith("# ")) {
                    addHeading(document, trimmed.substring(2), 16, true);
                } else if (trimmed.startsWith("- ")) {
                    addBullet(document, trimmed.substring(2));
                } else if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
                    addHeading(document, trimmed.replace("**", ""), 11, true);
                } else {
                    addParagraph(document, trimmed, 11, false);
                }
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    private static void addHeading(XWPFDocument document, String text, int fontSize, boolean bold) {
        addParagraph(document, text, fontSize, bold);
    }

    private static void addBullet(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = paragraph.createRun();
        run.setText("• " + text);
        run.setFontSize(11);
    }

    private static void addParagraph(XWPFDocument document, String text, int fontSize, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
    }

}
