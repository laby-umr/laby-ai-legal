package com.laby.module.legal.enums.clause;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同条款单元类型
 */
@Getter
@AllArgsConstructor
public enum LegalClauseTypeEnum {

    SECTION("SECTION", "章节"),
    CLAUSE("CLAUSE", "条款块"),
    TABLE("TABLE", "表格"),
    SIGNATURE("SIGNATURE", "签章区"),
    APPENDIX("APPENDIX", "附件");

    private final String code;
    private final String name;

}
