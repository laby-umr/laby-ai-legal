package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同导出可见性
 */
@Getter
@AllArgsConstructor
public enum LegalContractExportVisibilityEnum implements ArrayValuable<String> {

    INTERNAL("INTERNAL", "内部版"),
    EXTERNAL("EXTERNAL", "外发版"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractExportVisibilityEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static LegalContractExportVisibilityEnum valueOfCode(String code) {
        for (LegalContractExportVisibilityEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

}
