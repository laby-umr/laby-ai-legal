package com.laby.module.ai.service.knowledge.splitter;

import com.laby.module.ai.core.document.AiStructuredDocument;
import com.laby.module.ai.core.document.AiStructuredDocumentElement;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import com.laby.module.ai.framework.document.DocumentParseProperties;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeChunkBO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalKnowledgeChunkerTest {

    @Test
    void chunk_tableIsolatedFromText() {
        AiStructuredDocumentParseResult parseResult = buildParseResult(
                elem("title", "付款条款", 2, 1),
                elem("text", "买方应在验收后付款。", 2, 1),
                tableElem("|期次|比例|\n|---|---|\n|签约|30%|", "付款表", 2));

        HierarchicalKnowledgeChunker chunker = new HierarchicalKnowledgeChunker(properties());
        List<AiKnowledgeChunkBO> chunks = chunker.chunk(parseResult);

        AiKnowledgeChunkBO tableChunk = chunks.stream()
                .filter(chunk -> chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE)
                .findFirst()
                .orElseThrow();
        assertTrue(tableChunk.getContent().contains("|签约|30%|"));
        assertTrue(tableChunk.getEmbedText().contains("付款条款"));
        assertEquals(AiKnowledgeSegmentChunkLevelEnum.CHILD, tableChunk.getChunkLevel());

        assertTrue(chunks.stream().anyMatch(chunk ->
                chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW));
        assertTrue(chunks.stream().anyMatch(chunk ->
                chunk.getBlockType() == AiKnowledgeSegmentBlockTypeEnum.TABLE_SUMMARY));
    }

    @Test
    void chunk_createsParentChildRelation() {
        AiStructuredDocumentParseResult parseResult = buildParseResult(
                elem("title", "第一章", 2, 1),
                elem("text", "第一条 总则内容。", 2, 1),
                elem("text", "第二条 适用范围。", 2, 1));

        HierarchicalKnowledgeChunker chunker = new HierarchicalKnowledgeChunker(properties());
        List<AiKnowledgeChunkBO> chunks = chunker.chunk(parseResult);

        assertTrue(chunks.stream().anyMatch(chunk ->
                chunk.getChunkLevel() == AiKnowledgeSegmentChunkLevelEnum.PARENT));
        assertTrue(chunks.stream().anyMatch(chunk ->
                chunk.getChunkLevel() == AiKnowledgeSegmentChunkLevelEnum.CHILD
                        && StrUtil.isNotBlank(chunk.getParentTempKey())));
    }

    @Test
    void chunk_plainTextFallbackWhenNoElements() {
        AiStructuredDocumentParseResult parseResult = new AiStructuredDocumentParseResult()
                .setMarkdown("段落一\n\n段落二")
                .setQuality(AiDocumentParseQualityEnum.LOW);

        HierarchicalKnowledgeChunker chunker = new HierarchicalKnowledgeChunker(properties());
        List<AiKnowledgeChunkBO> chunks = chunker.chunk(parseResult);

        assertTrue(chunks.size() >= 1);
        assertEquals(AiKnowledgeSegmentBlockTypeEnum.TEXT, chunks.get(0).getBlockType());
    }

    private static DocumentParseProperties properties() {
        DocumentParseProperties properties = new DocumentParseProperties();
        properties.getStructuredChunk().setChildMaxTokens(128);
        properties.getStructuredChunk().setChildOverlapTokens(20);
        return properties;
    }

    private static AiStructuredDocumentParseResult buildParseResult(AiStructuredDocumentElement... elements) {
        return new AiStructuredDocumentParseResult()
                .setEngine(AiDocumentParseEngineEnum.MINERU)
                .setQuality(AiDocumentParseQualityEnum.HIGH)
                .setStructuredDocument(new AiStructuredDocument().setElements(List.of(elements)));
    }

    private static AiStructuredDocumentElement elem(String type, String text, int level, int page) {
        return new AiStructuredDocumentElement().setType(type).setText(text).setLevel(level).setPage(page);
    }

    private static AiStructuredDocumentElement tableElem(String markdown, String caption, int page) {
        return new AiStructuredDocumentElement().setType("table").setMarkdown(markdown).setCaption(caption).setPage(page);
    }

}
