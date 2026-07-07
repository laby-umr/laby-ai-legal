package com.laby.module.legal.enums.auditrule;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审核规则可执行类型
 */
@Getter
@AllArgsConstructor
public enum LegalAuditRuleTypeEnum implements ArrayValuable<String> {

    MANDATORY_CLAUSE("MANDATORY_CLAUSE", "必备条款"),
    FORBIDDEN_PATTERN("FORBIDDEN_PATTERN", "禁止表述"),
    PREFERRED_CLAUSE("PREFERRED_CLAUSE", "推荐标准条款"),
    CUSTOM_LLM("CUSTOM_LLM", "LLM 补充规则"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalAuditRuleTypeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    public static LegalAuditRuleTypeEnum of(String code) {
        if (code == null) {
            return CUSTOM_LLM;
        }
        for (LegalAuditRuleTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return CUSTOM_LLM;
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
