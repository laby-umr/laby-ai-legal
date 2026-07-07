package com.laby.module.ai.service.knowledge.splitter;

import com.laby.module.ai.core.document.AiStructuredDocument;
import com.laby.module.ai.core.document.AiStructuredDocumentElement;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.framework.document.DocumentParseProperties;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeChunkBO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpreadsheetTableChunkerTest {

    @Test
    void chunk_threeRowSheetProducesWholeRowAndSummary() {
        String tableMarkdown = """
                | Name | Age |
                | --- | --- |
                | John | 30 |
                | Jane | 25 |
                | Bob | 40 |
                """;
        AiStructuredDocumentParseResult parseResult = new AiStructuredDocumentParseResult()
                .setEngine(AiDocumentParseEngineEnum.SPREADSHEET)
                .setQuality(AiDocumentParseQualityEnum.HIGH)
                .setStructuredDocument(new AiStructuredDocument().setElements(List.of(
                        new AiStructuredDocumentElement()
                                .setType("table")
                                .setMarkdown(tableMarkdown)
                                .setCaption("Employees")
                                .setPage(1))));

        SpreadsheetTableChunker chunker = new SpreadsheetTableChunker(properties());
        List<AiKnowledgeChunkBO> chunks = chunker.chunk(parseResult);

        long wholeCount = chunks.stream()
                .filter(chunk -> chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE)
                .count();
        long rowCount = chunks.stream()
                .filter(chunk -> chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW)
                .count();
        long summaryCount = chunks.stream()
                .filter(chunk -> chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_SUMMARY)
                .count();

        assertEquals(1, wholeCount);
        assertEquals(3, rowCount);
        assertEquals(1, summaryCount);

        AiKnowledgeChunkBO whole = chunks.stream()
                .filter(chunk -> chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE)
                .findFirst()
                .orElseThrow();
        assertTrue(whole.getContent().contains("Employees"));
        assertTrue(whole.getContent().contains("| John | 30 |"));
        assertTrue(whole.getEmbedText().contains("Sheet: Employees"));

        assertTrue(chunks.stream()
                .filter(chunk -> chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW)
                .anyMatch(chunk -> chunk.getContent().contains("John")));
    }

    private static DocumentParseProperties properties() {
        DocumentParseProperties properties = new DocumentParseProperties();
        properties.getStructuredChunk().setTableRowIndexEnabled(true);
        properties.getStructuredChunk().setTableSummaryEnabled(true);
        return properties;
    }

}
