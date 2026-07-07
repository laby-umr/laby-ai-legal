package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum LegalPartyRoleEnum implements ArrayValuable<String> {

    PARTY_A("A", "甲方"),
    PARTY_B("B", "乙方"),
    OTHER("OTHER", "其他"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalPartyRoleEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
