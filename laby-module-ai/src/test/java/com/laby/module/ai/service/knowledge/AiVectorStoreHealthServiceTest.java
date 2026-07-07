package com.laby.module.ai.service.knowledge;

import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiVectorStoreHealthServiceTest {

    @Test
    void isMissingSparseText_shouldDetectBlankSparseWithContent() {
        AiKnowledgeSegmentDO segment = new AiKnowledgeSegmentDO()
                .setContent("保密义务条款")
                .setEmbedText("保密义务条款")
                .setSparseText(null);

        assertTrue(AiVectorStoreHealthService.isMissingSparseText(segment));
    }

    @Test
    void isMissingSparseText_shouldIgnoreWhenSparsePresent() {
        AiKnowledgeSegmentDO segment = new AiKnowledgeSegmentDO()
                .setContent("保密义务条款")
                .setSparseText("保密义务条款");

        assertFalse(AiVectorStoreHealthService.isMissingSparseText(segment));
    }

    @Test
    void isMissingSparseText_shouldIgnoreWhenNoSourceText() {
        AiKnowledgeSegmentDO segment = new AiKnowledgeSegmentDO()
                .setContent("")
                .setEmbedText(null)
                .setSparseText(null);

        assertFalse(AiVectorStoreHealthService.isMissingSparseText(segment));
    }

}
