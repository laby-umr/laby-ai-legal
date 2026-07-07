package com.laby.module.legal.service.contract.util;

import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalContractDocxRenderUtilTest {

    @Test
    void isAdoptApplicableToDocument_shouldNotUseSuggestionFallback() {
        LegalAuditOpinionDO opinion = LegalAuditOpinionDO.builder()
                .changeType(LegalOpinionChangeTypeEnum.NO_CHANGE.getCode())
                .suggestion("建议将包换期延长至6个月或1年")
                .build();
        assertFalse(LegalContractDocxRenderUtil.isAdoptApplicableToDocument(opinion));
    }

    @Test
    void isAdoptApplicableToDocument_shouldAcceptNewText() {
        LegalAuditOpinionDO opinion = LegalAuditOpinionDO.builder()
                .changeType(LegalOpinionChangeTypeEnum.REPLACE.getCode())
                .newText("包换期为6个月")
                .build();
        assertTrue(LegalContractDocxRenderUtil.isAdoptApplicableToDocument(opinion));
    }

}
