package com.laby.module.legal.service.contract.util;

import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalContractPdfAnnotateServiceTest {

    @Test
    void annotate_shouldWriteStandardTextAnnotations() throws Exception {
        byte[] original = LegalContractPdfTestFixtures.sampleContractPdf();
        LegalAuditOpinionDO opinion1 = LegalAuditOpinionDO.builder()
                .title("Confidentiality risk")
                .riskLevel(LegalRiskLevelEnum.HIGH.getCode())
                .content("Scope too broad")
                .suggestion("Limit confidential scope")
                .paragraphId("p-2")
                .oldText("trade secrets")
                .build();
        LegalAuditOpinionDO opinion2 = LegalAuditOpinionDO.builder()
                .title("Payment clause")
                .riskLevel(LegalRiskLevelEnum.MEDIUM.getCode())
                .content("Payment term unclear")
                .suggestion("Clarify payment schedule")
                .paragraphId("p-4")
                .oldText("Payment")
                .build();
        Map<String, LegalContractParagraphDO> paragraphMap = Map.of(
                "p-2", LegalContractParagraphDO.builder().paragraphId("p-2")
                        .text("Parties shall keep trade secrets confidential.").build(),
                "p-4", LegalContractParagraphDO.builder().paragraphId("p-4")
                        .text("Payment due in 30 days").build());

        LegalContractPdfAnnotateService service = new LegalContractPdfAnnotateService();
        byte[] annotated = service.annotate(original, List.of(opinion1, opinion2), paragraphMap);
        assertTrue(LegalContractPdfTestFixtures.countAnnotations(annotated) >= 2);
    }

}
