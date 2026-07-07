package com.laby.module.legal.service.playbook;

import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.enums.auditrule.LegalAuditMatchTypeEnum;
import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanRuleBO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalDeterministicAuditEngineTest {

    private final LegalDeterministicAuditEngine engine = new LegalDeterministicAuditEngine();

    @Test
    void run_shouldDetectMissingMandatoryClause() {
        LegalReviewPlanRuleBO rule = LegalReviewPlanRuleBO.builder()
                .ruleId(1L)
                .name("保密条款")
                .ruleType("MANDATORY_CLAUSE")
                .matchPattern("保密")
                .matchType(LegalAuditMatchTypeEnum.KEYWORD.getCode())
                .riskLevel("HIGH")
                .build();
        LegalReviewPlanBO plan = LegalReviewPlanBO.builder()
                .deterministicRules(List.of(rule))
                .build();

        List<LegalAuditOpinionDraftBO> opinions = engine.run(plan, List.of(), List.of());

        assertEquals(1, opinions.size());
        assertTrue(opinions.get(0).getTitle().contains("保密"));
        assertEquals("RULE", opinions.get(0).getSourceType());
    }

    @Test
    void run_shouldDetectForbiddenPattern() {
        LegalReviewPlanRuleBO rule = LegalReviewPlanRuleBO.builder()
                .ruleId(2L)
                .name("无限责任")
                .ruleType("FORBIDDEN_PATTERN")
                .matchPattern("无限.*责任")
                .matchType(LegalAuditMatchTypeEnum.REGEX.getCode())
                .riskLevel("HIGH")
                .build();
        LegalReviewPlanBO plan = LegalReviewPlanBO.builder()
                .deterministicRules(List.of(rule))
                .build();

        var paragraph = new LegalContractParagraphDO();
        paragraph.setParagraphId("p-1");
        paragraph.setText("乙方承担无限责任。");

        List<LegalAuditOpinionDraftBO> opinions = engine.run(plan, List.of(), List.of(paragraph));

        assertEquals(1, opinions.size());
        assertEquals("p-1", opinions.get(0).getParagraphId());
    }

}
