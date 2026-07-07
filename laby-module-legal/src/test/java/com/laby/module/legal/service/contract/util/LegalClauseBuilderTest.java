package com.laby.module.legal.service.contract.util;

import com.laby.module.legal.service.contract.util.LegalContractWordParser.ParagraphItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalClauseBuilderTest {

    @Test
    void build_shouldGroupNonHeadingParagraphsIntoClause() {
        List<ParagraphItem> paragraphs = List.of(
                new ParagraphItem("p-1", 1, "这是合同前言第一段。", null),
                new ParagraphItem("p-2", 2, "这是第二段正文。", null),
                new ParagraphItem("p-3", 3, "第一条 付款", null),
                new ParagraphItem("p-4", 4, "买方应在30日内付款。", null)
        );

        var clauses = LegalClauseBuilder.build(paragraphs, null);

        assertEquals(2, clauses.size());
        assertEquals("c-1", clauses.get(0).getClauseId());
        assertEquals(2, clauses.get(0).getParagraphIds().size());
        assertEquals("c-2", clauses.get(1).getClauseId());
        assertEquals("第一条 付款", clauses.get(1).getTitle());
        assertTrue(clauses.get(1).getFullText().contains("30日内"));
    }

    @Test
    void isHeading_shouldDetectCnArticle() {
        assertTrue(LegalClauseBuilder.isHeading(null, "第一条 保密义务"));
        assertTrue(LegalClauseBuilder.isHeading(null, "1.2.3 交付条款"));
    }

}
