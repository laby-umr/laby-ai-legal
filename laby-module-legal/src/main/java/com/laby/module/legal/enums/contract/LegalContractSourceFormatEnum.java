package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同原件格式
 */
@Getter
@AllArgsConstructor
public enum LegalContractSourceFormatEnum implements ArrayValuable<String> {

    DOCX("DOCX"),
    DOC("DOC"),
    PDF("PDF");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractSourceFormatEnum::getFormat)
            .toArray(String[]::new);

    private final String format;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static LegalContractSourceFormatEnum of(String format) {
        if (format == null) {
            return null;
        }
        for (LegalContractSourceFormatEnum item : values()) {
            if (item.format.equalsIgnoreCase(format)) {
                return item;
            }
        }
        return null;
    }
}
