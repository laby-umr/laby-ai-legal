package com.laby.module.legal.enums.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合规检查结论（审核报告第四章）
 */
@Getter
@AllArgsConstructor
public enum LegalComplianceResultEnum {

    PASS("通过"),
    WARNING("警告"),
    FAIL("不通过"),
    ;

    private final String label;

    public static LegalComplianceResultEnum fromRiskLevel(LegalRiskLevelEnum riskLevel) {
        return switch (riskLevel) {
            case HIGH -> FAIL;
            case MEDIUM -> WARNING;
            default -> PASS;
        };
    }

}
