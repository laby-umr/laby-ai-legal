package com.laby.module.legal.service.contract.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 测试用 PDF 合同样本生成。
 */
public final class LegalContractPdfTestFixtures {

    private static final List<String> DEFAULT_CLAUSES = List.of(
            "第一条 保密义务",
            "双方对商业秘密、技术资料负有保密义务，未经披露方书面同意不得向第三方披露。",
            "第二条 付款与违约金",
            "买方应在合同签订后三十日内付款；逾期付款的，应按日支付违约金。",
            "第三条 争议解决与仲裁",
            "因本合同引起的争议，双方应友好协商；协商不成的，提交北京仲裁委员会仲裁。",
            "第四条 不可抗力",
            "因不可抗力不能履行合同的，根据不可抗力的影响，部分或全部免除责任。",
            "第五条 知识产权",
            "本合同项下知识产权的归属与侵权责任按双方约定执行。");

    private LegalContractPdfTestFixtures() {
    }

    public static byte[] sampleContractPdf() throws IOException {
        try {
            byte[] chinesePdf = tryBuildChineseContractPdf();
            if (chinesePdf != null) {
                return chinesePdf;
            }
        } catch (Exception ignored) {
            // TTC / 字体环境不可用时降级英文样本
        }
        return buildEnglishContractPdf();
    }

    public static byte[] sampleChineseContractPdfOrEnglish() throws IOException {
        return sampleContractPdf();
    }

    private static byte[] buildEnglishContractPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
            float y = 720;
            float margin = 50;
            for (String line : List.of(
                    "Article 1 Confidentiality",
                    "Parties shall keep trade secrets confidential.",
                    "Article 2 Payment and penalty",
                    "Payment due in 30 days; late payment incurs penalty.",
                    "Article 3 Arbitration",
                    "Disputes shall be resolved by arbitration.",
                    "Article 4 Force majeure",
                    "Force majeure events may excuse performance.",
                    "Article 5 Intellectual property",
                    "Intellectual property rights belong to the parties.")) {
                stream.beginText();
                stream.newLineAtOffset(margin, y);
                stream.showText(line);
                stream.endText();
                y -= 22;
            }
            stream.close();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static byte[] tryBuildChineseContractPdf() throws IOException {
        File fontFile = resolveChineseFontFile();
        if (fontFile == null) {
            return null;
        }
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            PDType0Font font = PDType0Font.load(document, fontFile);
            stream.setFont(font, 11);
            float y = 720;
            float margin = 50;
            for (String line : DEFAULT_CLAUSES) {
                stream.beginText();
                stream.newLineAtOffset(margin, y);
                stream.showText(line);
                stream.endText();
                y -= 22;
                if (y < 72) {
                    break;
                }
            }
            stream.close();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static File resolveChineseFontFile() {
        List<String> candidates = List.of(
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/msyh.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/System/Library/Fonts/PingFang.ttc");
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                return path.toFile();
            }
        }
        return null;
    }

    public static void writeSampleContractPdf(Path target) throws IOException {
        Files.write(target, sampleContractPdf());
    }

    public static int countAnnotations(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int count = 0;
            for (PDPage page : document.getPages()) {
                count += page.getAnnotations().size();
            }
            return count;
        }
    }

}
