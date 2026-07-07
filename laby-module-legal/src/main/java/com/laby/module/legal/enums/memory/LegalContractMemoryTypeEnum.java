package com.laby.module.legal.enums.memory;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同情节记忆类型
 */
@Getter
@AllArgsConstructor
public enum LegalContractMemoryTypeEnum implements ArrayValuable<String> {

    MILESTONE("milestone"),
    RISK("risk"),
    DECISION("decision"),
    FACT("fact"),
    COMPACTION_SUMMARY("compaction_summary"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractMemoryTypeEnum::getType).toArray(String[]::new);

    private final String type;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
