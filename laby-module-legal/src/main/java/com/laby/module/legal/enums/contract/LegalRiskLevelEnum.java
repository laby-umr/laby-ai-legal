package com.laby.module.legal.enums.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;

/**
 * AI 审核风险等级
 */
@Getter
@AllArgsConstructor
public enum LegalRiskLevelEnum implements ArrayValuable<String> {

    HIGH("HIGH", "高", 10),
    MEDIUM("MEDIUM", "中", 5),
    LOW("LOW", "低", 2),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalRiskLevelEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;
    /** 单项扣分（报告第五章，取负数展示） */
    private final int deductScore;

    public static LegalRiskLevelEnum normalize(String raw) {
        if (StrUtil.isBlank(raw)) {
            return MEDIUM;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        for (LegalRiskLevelEnum item : values()) {
            if (item.code.equals(upper)) {
                return item;
            }
        }
        if (upper.contains("高")) {
            return HIGH;
        }
        if (upper.contains("低")) {
            return LOW;
        }
        return MEDIUM;
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
