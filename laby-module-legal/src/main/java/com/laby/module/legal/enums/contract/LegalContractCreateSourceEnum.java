package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同创建来源（字典 legal_contract_create_source）
 */
@Getter
@AllArgsConstructor
public enum LegalContractCreateSourceEnum implements ArrayValuable<String> {

    MANUAL("MANUAL", "人工创建"),
    AI_CHAT("AI_CHAT", "AI 对话创建"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractCreateSourceEnum::getSource).toArray(String[]::new);

    private final String source;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
