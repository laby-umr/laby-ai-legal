package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同采纳版导出模式
 */
@Getter
@AllArgsConstructor
public enum LegalContractExportModeEnum implements ArrayValuable<String> {

    TRACKED("TRACKED", "带修订"),
    CLEAN("CLEAN", "干净版"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractExportModeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static LegalContractExportModeEnum valueOfCode(String code) {
        for (LegalContractExportModeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

}
