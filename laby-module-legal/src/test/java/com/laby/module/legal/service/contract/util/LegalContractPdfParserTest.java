package com.laby.module.legal.service.contract.util;

import com.laby.module.legal.service.contract.util.LegalContractWordParser.ParagraphItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalContractPdfParserTest {

    @Test
    void parse_shouldExtractParagraphsFromTextLayer() throws Exception {
        byte[] pdfBytes = LegalContractPdfTestFixtures.sampleContractPdf();
        LegalContractPdfParser.ParseResult result = LegalContractPdfParser.parse(pdfBytes);
        assertFalse(result.isScanLikely());
        List<ParagraphItem> paragraphs = result.getParagraphs();
        assertFalse(paragraphs.isEmpty());
        assertTrue(paragraphs.get(0).getParagraphId().startsWith("p-"));
    }

    @Test
    void splitParagraphs_shouldSplitClauseBlocks() {
        String text = "第一条 保密义务\n双方应保守商业秘密。\n\n第二条 付款期限\n逾期须支付违约金。";
        List<ParagraphItem> items = LegalContractPdfParser.splitParagraphs(text);
        assertTrue(items.size() >= 2);
    }

}
