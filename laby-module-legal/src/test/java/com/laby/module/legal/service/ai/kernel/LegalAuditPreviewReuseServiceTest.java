package com.laby.module.legal.service.ai.kernel;

import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegalAuditPreviewReuseServiceTest {

    private final LegalAuditPreviewReuseService reuseService = new LegalAuditPreviewReuseService();

    @Test
    void merge_shouldPreferFormalAndFillFromPreview() {
        LegalAiAuditOpinionItemBO formal = new LegalAiAuditOpinionItemBO();
        formal.setParagraphId("p-1");
        formal.setTitle("付款条款风险");
        formal.setContent("正式内容");
        formal.setSourceType(LegalOpinionSourceTypeEnum.AI.getCode());

        LegalAiAuditOpinionItemBO previewDup = new LegalAiAuditOpinionItemBO();
        previewDup.setParagraphId("p-1");
        previewDup.setTitle("付款条款风险");
        previewDup.setSourceType(LegalOpinionSourceTypeEnum.PREVIEW.getCode());

        LegalAiAuditOpinionItemBO previewOnly = new LegalAiAuditOpinionItemBO();
        previewOnly.setParagraphId("p-2");
        previewOnly.setTitle("保密条款缺失");
        previewOnly.setSourceType(LegalOpinionSourceTypeEnum.PREVIEW.getCode());

        var result = reuseService.merge(List.of(formal), List.of(previewDup, previewOnly));

        assertEquals(2, result.getItems().size());
        assertEquals(1, result.getFormalCount());
        assertEquals(2, result.getPreviewCount());
        assertEquals(1, result.getDedupeCount());
        assertEquals(1, result.getReusedFromPreviewCount());
        assertEquals("正式内容", result.getItems().get(0).getContent());
    }

    @Test
    void dedupeKey_shouldNormalizeTitle() {
        LegalAiAuditOpinionItemBO item = new LegalAiAuditOpinionItemBO();
        item.setParagraphId("p-1");
        item.setTitle("  Risk Title ");
        assertEquals("p-1|risk title", LegalAuditPreviewReuseService.dedupeKey(item));
    }

}
