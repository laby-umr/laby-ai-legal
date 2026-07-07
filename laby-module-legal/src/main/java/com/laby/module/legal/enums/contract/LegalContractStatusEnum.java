package com.laby.module.legal.enums.contract;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 法务合同业务状态
 */
@Getter
@AllArgsConstructor
public enum LegalContractStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PARSING(10, "解析中"),
    AI_AUDITING(11, "AI 审核中"),
    FAILED(15, "处理失败"),
    OPINION_REVIEW(20, "意见处置"),
    AI_REAUDITING(21, "AI 二轮审核中"),
    DIRECTOR_REVIEW(30, "总监确认"),
    FINALIZING(40, "人工收尾"),
    ARCHIVED(50, "已归档"),
    REJECTED(60, "已驳回"),
    CANCELLED(61, "已取消"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(LegalContractStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
