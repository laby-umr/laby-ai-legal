package com.laby.module.legal.service.contract.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.service.opinion.LegalAuditOpinionAnnotationFormatter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * PDF 标准批注导出（ISO 32000 Text 注释，不改原件正文流）。
 */
@Service
public class LegalContractPdfAnnotateService {

    private static final float ICON_WIDTH = 24;
    private static final float ICON_HEIGHT = 24;

    public byte[] annotate(byte[] originalPdf, List<LegalAuditOpinionDO> opinions,
                           Map<String, LegalContractParagraphDO> paragraphMap) throws IOException {
        if (originalPdf == null || originalPdf.length == 0) {
            throw new IOException("PDF 原件为空");
        }
        if (CollUtil.isEmpty(opinions)) {
            return originalPdf;
        }
        try (PDDocument document = Loader.loadPDF(originalPdf);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int sideIndex = 0;
            for (LegalAuditOpinionDO opinion : opinions) {
                if (opinion == null) {
                    continue;
                }
                addTextAnnotation(document, originalPdf, opinion, paragraphMap, sideIndex++);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void addTextAnnotation(PDDocument document, byte[] originalPdf, LegalAuditOpinionDO opinion,
                                   Map<String, LegalContractParagraphDO> paragraphMap, int sideIndex) throws IOException {
        LegalContractParagraphDO paragraph = paragraphMap != null
                ? paragraphMap.get(opinion.getParagraphId()) : null;
        String anchorText = LegalContractPdfTextLocator.pickAnchorText(
                opinion.getOldText(), paragraph != null ? paragraph.getText() : null);
        LegalContractPdfTextLocator.TextAnchor anchor = LegalContractPdfTextLocator.locate(originalPdf, anchorText);

        int pageIndex = anchor != null ? anchor.getPageIndex() : 0;
        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            pageIndex = 0;
        }
        PDPage page = document.getPage(pageIndex);
        PDRectangle mediaBox = page.getMediaBox();

        float x = anchor != null ? anchor.getX() : mediaBox.getWidth() - 36;
        float y = anchor != null ? anchor.getY() : mediaBox.getHeight() - 72 - (sideIndex * 32);
        if (y < 48) {
            y = mediaBox.getHeight() - 72;
        }

        PDAnnotationText annotation = new PDAnnotationText();
        annotation.setRectangle(new PDRectangle(x, y, ICON_WIDTH, ICON_HEIGHT));
        String title = StrUtil.blankToDefault(opinion.getTitle(), "审核意见");
        annotation.setContents(title + "\n" + LegalAuditOpinionAnnotationFormatter.previewText(opinion));
        annotation.setSubject(title);
        page.getAnnotations().add(annotation);
    }

}
