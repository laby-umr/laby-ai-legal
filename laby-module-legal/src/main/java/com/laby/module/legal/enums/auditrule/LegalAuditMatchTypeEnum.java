package com.laby.module.legal.enums.auditrule;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 规则匹配方式
 */
@Getter
@AllArgsConstructor
public enum LegalAuditMatchTypeEnum {

    KEYWORD("KEYWORD"),
    REGEX("REGEX"),
    SEMANTIC("SEMANTIC"),
    ;

    private final String code;

    public static LegalAuditMatchTypeEnum of(String code) {
        if (code == null) {
            return KEYWORD;
        }
        for (LegalAuditMatchTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return KEYWORD;
    }

}
