package com.laby.module.legal.enums.opinion;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审核意见来源类型
 */
@Getter
@AllArgsConstructor
public enum LegalOpinionSourceTypeEnum implements ArrayValuable<String> {

    AI("AI", "AI"),
    PREVIEW("PREVIEW", "编排预览"),
    RULE("RULE", "审核规则"),
    STANDARD_CLAUSE("STANDARD_CLAUSE", "标准条款"),
    KNOWLEDGE("KNOWLEDGE", "知识库"),
    MANUAL("MANUAL", "人工"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalOpinionSourceTypeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
