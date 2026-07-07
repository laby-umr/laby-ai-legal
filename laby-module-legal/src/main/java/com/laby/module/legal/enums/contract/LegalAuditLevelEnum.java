package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同审核强度
 */
@Getter
@AllArgsConstructor
public enum LegalAuditLevelEnum implements ArrayValuable<String> {

    STANDARD("standard", "标准"),
    STRICT("strict", "严格"),
    RELAXED("relaxed", "宽松"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalAuditLevelEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
