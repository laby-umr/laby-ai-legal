package com.laby.module.legal.service.opinion;

import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalAuditOpinionAnnotationFormatterTest {

    @Test
    void previewText_shouldUseStructuredSectionsNotKeyValuePairs() {
        String preview = LegalAuditOpinionAnnotationFormatter.previewText(LegalAuditOpinionDO.builder()
                .riskLevel("MEDIUM")
                .title("保证金退还条件模糊")
                .content("合同未明确违约时保证金不予退还的具体情形。")
                .suggestion("细化违约情形，明确不予退还的具体条件。")
                .oldText("保证金在合同终止后30日内退还")
                .newText("除乙方严重违约且经书面通知后仍未纠正的情形外，保证金应在合同终止后30日内退还。")
                .paragraphId("p-12")
                .sourceType("AI")
                .auditRound(1)
                .changeType(LegalOpinionChangeTypeEnum.REPLACE.getCode())
                .build());

        assertTrue(preview.contains("【MEDIUM】保证金退还条件模糊"));
        assertTrue(preview.contains("风险说明："));
        assertTrue(preview.contains("修改说明："));
        assertTrue(preview.contains("采纳后改写"));
        assertTrue(preview.contains("原文："));
        assertTrue(preview.contains("改后正文："));
        assertTrue(preview.contains("段落 p-12"));
        assertFalse(preview.contains("风险=MEDIUM"));
        assertFalse(preview.contains("；标题="));
    }

}
