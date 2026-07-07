package com.laby.module.legal.enums.opinion;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审核意见条款类型（AI 输出 + 人工补充）
 */
@Getter
@AllArgsConstructor
public enum LegalClauseTypeEnum implements ArrayValuable<String> {

    MANUAL("MANUAL", "人工补充"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalClauseTypeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
