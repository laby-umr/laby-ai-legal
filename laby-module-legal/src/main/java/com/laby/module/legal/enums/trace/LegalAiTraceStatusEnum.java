package com.laby.module.legal.enums.trace;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI 链路追踪状态
 */
@Getter
@AllArgsConstructor
public enum LegalAiTraceStatusEnum implements ArrayValuable<String> {

    RUNNING("RUNNING", "运行中"),
    SUCCESS("SUCCESS", "成功"),
    FAIL("FAIL", "失败"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalAiTraceStatusEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    public static LegalAiTraceStatusEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (LegalAiTraceStatusEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
