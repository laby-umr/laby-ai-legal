package com.laby.module.legal.enums.ai;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审核内核运行模式
 */
@Getter
@AllArgsConstructor
public enum LegalAuditKernelModeEnum implements ArrayValuable<String> {

    FORMAL("FORMAL", "正式审核"),
    PREVIEW("PREVIEW", "编排预览"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalAuditKernelModeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
