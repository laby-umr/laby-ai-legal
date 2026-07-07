package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同附件角色
 */
@Getter
@AllArgsConstructor
public enum LegalContractFileRoleEnum implements ArrayValuable<String> {

    ORIGINAL("ORIGINAL"),
    NORMALIZED_DOCX("NORMALIZED_DOCX"),
    PREVIEW_PDF("PREVIEW_PDF"),
    ANNOTATED_PDF("ANNOTATED_PDF"),
    /** 归档发布 ZIP（四件套 + 报告 + manifest） */
    PUBLISHED_BUNDLE("PUBLISHED_BUNDLE");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractFileRoleEnum::getRole)
            .toArray(String[]::new);

    private final String role;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
