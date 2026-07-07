package com.laby.module.legal.service.auditrule;

import com.laby.framework.common.exception.ServiceException;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRuleSaveReqVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CFG-001：审核规则业务校验单测
 */
class LegalAuditRuleServiceImplTest {

    @Test
    void validateRuleBusinessRules_preferredClauseRequiresStandardClause() {
        LegalAuditRuleSaveReqVO reqVO = new LegalAuditRuleSaveReqVO();
        reqVO.setRuleType("PREFERRED_CLAUSE");
        reqVO.setName("测试推荐条款");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> LegalAuditRuleServiceImpl.validateRuleBusinessRules(reqVO));
        assertEquals(1_050_000_061, ex.getCode());
    }

    @Test
    void validateRuleBusinessRules_preferredClauseWithStandardClauseOk() {
        LegalAuditRuleSaveReqVO reqVO = new LegalAuditRuleSaveReqVO();
        reqVO.setRuleType("PREFERRED_CLAUSE");
        reqVO.setStandardClauseId(1L);

        assertDoesNotThrow(() -> LegalAuditRuleServiceImpl.validateRuleBusinessRules(reqVO));
    }

    @Test
    void validateRuleBusinessRules_customLlmWithoutStandardClauseOk() {
        LegalAuditRuleSaveReqVO reqVO = new LegalAuditRuleSaveReqVO();
        reqVO.setRuleType("CUSTOM_LLM");

        assertDoesNotThrow(() -> LegalAuditRuleServiceImpl.validateRuleBusinessRules(reqVO));
    }

    @Test
    void validateRuleBusinessRules_invalidRuleTypeRejected() {
        LegalAuditRuleSaveReqVO reqVO = new LegalAuditRuleSaveReqVO();
        reqVO.setRuleType("PREFERRED_CLAUSE_TYPO");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> LegalAuditRuleServiceImpl.validateRuleBusinessRules(reqVO));
        assertEquals(1_050_000_062, ex.getCode());
    }

}
