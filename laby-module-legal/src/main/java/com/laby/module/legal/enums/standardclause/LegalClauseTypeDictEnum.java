package com.laby.module.legal.enums.standardclause;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 标准条款 / 审核规则常用的条款分类（字典 legal_clause_type，存库为 label 文本）
 */
@Getter
@AllArgsConstructor
public enum LegalClauseTypeDictEnum implements ArrayValuable<String> {

    CONFIDENTIALITY("保密", "保密"),
    BREACH("违约", "违约责任"),
    IP("知产", "知识产权"),
    PAYMENT("价款", "价款支付"),
    DISPUTE("争议", "争议解决"),
    OTHER("其他", "其他"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalClauseTypeDictEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
