package com.laby.module.legal.enums.skillpack;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * SkillPack 适用场景
 */
@Getter
@AllArgsConstructor
public enum LegalSkillPackSceneEnum implements ArrayValuable<String> {

    AUDIT("AUDIT", "合同审核"),
    CHAT("CHAT", "合同问答"),
    PROPOSE("PROPOSE", "Agent 提案"),
    EXPORT_SUMMARY("EXPORT_SUMMARY", "导出摘要"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalSkillPackSceneEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String name;

    public static LegalSkillPackSceneEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (LegalSkillPackSceneEnum item : values()) {
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
