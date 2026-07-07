package com.laby.module.legal.enums.opinion;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审核意见改写类型
 */
@Getter
@AllArgsConstructor
public enum LegalOpinionChangeTypeEnum implements ArrayValuable<String> {

    REPLACE("REPLACE", "替换"),
    INSERT_BEFORE("INSERT_BEFORE", "前插入"),
    INSERT_AFTER("INSERT_AFTER", "后插入"),
    DELETE("DELETE", "删除"),
    NO_CHANGE("NO_CHANGE", "仅提示"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalOpinionChangeTypeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
