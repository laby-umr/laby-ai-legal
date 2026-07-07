package com.laby.module.legal.enums.standardclause;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 标准条款适用范围
 */
@Getter
@AllArgsConstructor
public enum LegalStandardClauseScopeEnum implements ArrayValuable<String> {

    COMMON("COMMON", "通用"),
    BG("BG", "事业群"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalStandardClauseScopeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    public static boolean isValid(String code) {
        if (StrUtil.isBlank(code)) {
            return false;
        }
        for (LegalStandardClauseScopeEnum item : values()) {
            if (item.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
