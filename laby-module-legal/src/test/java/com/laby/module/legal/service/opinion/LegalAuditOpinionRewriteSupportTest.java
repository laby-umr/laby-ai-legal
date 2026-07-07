package com.laby.module.legal.service.opinion;

import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalAuditOpinionRewriteSupportTest {

    @Test
    void extractRewriteTextFromSuggestion_shouldParseModifyPrefix() {
        assertEquals(
                "乙方未按时交货的，每逾期一日应向甲方支付万分之五的违约金。",
                LegalAuditOpinionRewriteSupport.extractRewriteTextFromSuggestion(
                        "建议修改为：乙方未按时交货的，每逾期一日应向甲方支付万分之五的违约金。"));
    }

    @Test
    void extractRewriteTextFromSuggestion_shortAdviceReturnsEmpty() {
        assertEquals("",
                LegalAuditOpinionRewriteSupport.extractRewriteTextFromSuggestion("建议将包换期延长至6个月或1年"));
    }

    @Test
    void normalizeAiOpinionItem_shouldUpgradeToReplaceWhenNewTextPresent() {
        LegalAiAuditOpinionItemBO item = new LegalAiAuditOpinionItemBO();
        item.setSuggestion("建议修改为：包换期为6个月");
        item.setParagraphId("p-10");
        LegalAuditOpinionRewriteSupport.normalizeAiOpinionItem(item, "产品包换期仅3个月。");
        assertEquals(LegalOpinionChangeTypeEnum.REPLACE.getCode(), item.getChangeType());
        assertEquals("包换期为6个月", item.getNewText());
    }

    @Test
    void isAdoptApplicableToDocument_shouldRejectSuggestionOnly() {
        LegalAuditOpinionDO opinion = LegalAuditOpinionDO.builder()
                .changeType(LegalOpinionChangeTypeEnum.NO_CHANGE.getCode())
                .suggestion("建议将包换期延长至6个月或1年")
                .build();
        assertFalse(LegalAuditOpinionRewriteSupport.isAdoptApplicableToDocument(opinion));
    }

    @Test
    void normalizeAiOpinionItem_missingClauseWithReference_shouldInsertAfter() {
        LegalAiAuditOpinionItemBO item = new LegalAiAuditOpinionItemBO();
        item.setTitle("缺少付款条款");
        item.setContent("合同未约定付款方式、期限及条件。");
        item.setSuggestion("建议补充付款方式、期限及条件。");
        item.setParagraphId("p-20");
        item.setReferenceClause("付款方式及期限：甲方应于收到发票后30日内以银行转账方式支付全部合同价款。");
        LegalAuditOpinionRewriteSupport.normalizeAiOpinionItem(item, "第八条 其他");
        assertEquals(LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode(), item.getChangeType());
        assertTrue(item.getNewText().contains("付款方式"));
    }

    @Test
    void isAdoptApplicableToDocument_shouldAcceptInsertAfterWithNewTextOnly() {
        LegalAuditOpinionDO opinion = LegalAuditOpinionDO.builder()
                .changeType(LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode())
                .newText("付款方式及期限：甲方应于收到发票后30日内支付。")
                .build();
        assertTrue(LegalAuditOpinionRewriteSupport.isAdoptApplicableToDocument(opinion));
    }

    @Test
    void isAdoptApplicableToDocument_shouldAcceptReplaceWithNewText() {
        LegalAuditOpinionDO opinion = LegalAuditOpinionDO.builder()
                .changeType(LegalOpinionChangeTypeEnum.REPLACE.getCode())
                .oldText("包换期3个月")
                .newText("包换期为6个月")
                .build();
        assertTrue(LegalAuditOpinionRewriteSupport.isAdoptApplicableToDocument(opinion));
    }

}
