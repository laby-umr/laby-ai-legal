package com.laby.module.ai.service.knowledge.support;

import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiKnowledgeSegmentSearchContextSupportTest {

    @Test
    void enrich_backfillsParentContent() {
        AiKnowledgeSegmentDO parent = new AiKnowledgeSegmentDO()
                .setId(10L)
                .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.PARENT.getLevel())
                .setContent("第一章 总则\n第一条 合同目的。");
        AiKnowledgeSegmentDO child = new AiKnowledgeSegmentDO()
                .setId(11L)
                .setParentId(10L)
                .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD.getLevel())
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TEXT.getCode())
                .setContent("第一条 合同目的。");

        AiKnowledgeSegmentSearchRespBO hit = new AiKnowledgeSegmentSearchRespBO();
        hit.setId(11L);
        hit.setContent(child.getContent());
        hit.setScore(0.9D);

        List<AiKnowledgeSegmentSearchRespBO> enriched = AiKnowledgeSegmentSearchContextSupport.enrich(
                List.of(hit),
                List.of(child),
                ids -> List.of(parent),
                null,
                true);

        assertTrue(enriched.get(0).getRetrievalContent().contains("第一章 总则"));
        assertTrue(enriched.get(0).getRetrievalContent().contains("第一条 合同目的"));
    }

    @Test
    void enrich_attachesTableWholeForRowHit() {
        AiKnowledgeSegmentDO whole = new AiKnowledgeSegmentDO()
                .setId(20L)
                .setDocumentId(1L)
                .setHeadingPath("付款条款")
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE.getCode())
                .setContent("付款表\n|期次|比例|\n|签约|30%|");
        AiKnowledgeSegmentDO row = new AiKnowledgeSegmentDO()
                .setId(21L)
                .setDocumentId(1L)
                .setHeadingPath("付款条款")
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW.getCode())
                .setContent("{\"期次\":\"签约\",\"比例\":\"30%\"}");

        AiKnowledgeSegmentSearchRespBO hit = new AiKnowledgeSegmentSearchRespBO();
        hit.setId(21L);
        hit.setContent(row.getContent());
        hit.setScore(0.88D);

        List<AiKnowledgeSegmentSearchRespBO> enriched = AiKnowledgeSegmentSearchContextSupport.enrich(
                List.of(hit),
                List.of(row),
                ids -> List.of(),
                (documentId, headingPath) -> whole,
                true);

        assertTrue(enriched.get(0).getRetrievalContent().contains("|签约|30%|"));
        assertTrue(enriched.get(0).getRetrievalContent().contains("签约"));
    }

}
