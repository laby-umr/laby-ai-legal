package com.laby.module.legal.enums.opinion;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 法务审核意见处置状态
 */
@Getter
@AllArgsConstructor
public enum LegalOpinionStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待处置"),
    ADOPTED(1, "已采纳"),
    IGNORED(2, "已忽略"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(LegalOpinionStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
