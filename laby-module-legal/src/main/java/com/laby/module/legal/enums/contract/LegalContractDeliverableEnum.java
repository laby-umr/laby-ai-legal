package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同四件套交付物类型（DELIV-001）。
 *
 * <p>用户下载区固定四项：源文件 / 标注版 / 修订版 / 采纳版。</p>
 */
@Getter
@AllArgsConstructor
public enum LegalContractDeliverableEnum implements ArrayValuable<String> {

    ORIGINAL("ORIGINAL", "源文件"),
    ANNOTATED("ANNOTATED", "标注版"),
    REVISION("REVISION", "修订版"),
    ADOPTED("ADOPTED", "采纳版"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalContractDeliverableEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    /**
     * 解析交付物参数，忽略大小写。
     *
     * @param code 请求参数 deliverable
     * @return 枚举；无法识别时返回 {@code null}
     */
    public static LegalContractDeliverableEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (LegalContractDeliverableEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

}
