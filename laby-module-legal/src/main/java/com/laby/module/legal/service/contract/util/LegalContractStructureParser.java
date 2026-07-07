package com.laby.module.legal.service.contract.util;

import com.laby.module.legal.service.contract.bo.LegalClauseUnitBO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * docx 结构解析门面：段落（稳定 p-n）+ 条款树 + 表格块。
 */
public final class LegalContractStructureParser {

    private LegalContractStructureParser() {
    }

    public static StructureParseResult parse(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<LegalContractWordParser.ParagraphItem> paragraphs =
                    LegalContractWordParser.parseParagraphs(document);
            List<XWPFParagraph> poiParagraphs = document.getParagraphs();
            List<LegalClauseUnitBO> clauses = new ArrayList<>(
                    LegalClauseBuilder.build(paragraphs, poiParagraphs));
            int nextSort = clauses.stream()
                    .map(LegalClauseUnitBO::getSort)
                    .max(Integer::compareTo)
                    .orElse(0);
            clauses.addAll(LegalTableExtractor.extract(document, nextSort));
            return new StructureParseResult(paragraphs, clauses);
        }
    }

    @Data
    @AllArgsConstructor
    public static class StructureParseResult {
        private List<LegalContractWordParser.ParagraphItem> paragraphs;
        private List<LegalClauseUnitBO> clauses;
    }

}
