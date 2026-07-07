package com.laby.module.ai.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 无引用守卫策略
 */
@AllArgsConstructor
@Getter
public enum AiRagNoAnswerPolicyEnum {

    STRICT("strict", "无召回则固定拒答"),
    RELAXED("relaxed", "无召回则模型自由回答"),
    HINT("hint", "无召回则提示用户补充关键词");

    private final String code;
    private final String name;

    public static AiRagNoAnswerPolicyEnum valueOfCode(String code) {
        if (code == null) {
            return STRICT;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(STRICT);
    }

}
