package com.laby.module.legal.dal.dataobject.standardclause;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务标准条款库 DO
 */
@TableName("legal_standard_clause")
@KeySequence("legal_standard_clause_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalStandardClauseDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 条款名称
     */
    private String name;
    /**
     * 条款分类
     */
    private String clauseType;
    /**
     * 适用范围
     *
     * 枚举类 {@link com.laby.module.legal.enums.standardclause.LegalStandardClauseScopeEnum}
     */
    private String categoryScope;
    /**
     * 标准条款正文
     */
    private String content;
    /**
     * 来源说明
     */
    private String referenceSource;
    /**
     * 状态 0 启用 1 禁用
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;

}
