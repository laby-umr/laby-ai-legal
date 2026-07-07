package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同版本类型
 */
@Getter
@AllArgsConstructor
public enum LegalContractVersionTypeEnum implements ArrayValuable<String> {

    ORIGINAL("ORIGINAL", "原始版"),
    WORKING("WORKING", "工作版"),
    AI_ANNOTATED("AI_ANNOTATED", "标注版"),
    ADOPTED_TRACKED("ADOPTED_TRACKED", "采纳版-带修订"),
    ADOPTED_CLEAN("ADOPTED_CLEAN", "采纳版-干净"),
    /** 归档发布包（immutable ZIP，DELIV-001 §17） */
    PUBLISHED("PUBLISHED", "归档发布包"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractVersionTypeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
