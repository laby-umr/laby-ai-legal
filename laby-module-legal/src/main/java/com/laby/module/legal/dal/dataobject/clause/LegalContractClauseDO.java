package com.laby.module.legal.dal.dataobject.clause;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务合同条款结构 DO
 */
@TableName("legal_contract_clause")
@KeySequence("legal_contract_clause_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractClauseDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long contractId;

    /**
     * 条款业务 ID，如 c-1
     */
    private String clauseId;

    private String parentClauseId;

    private Integer sort;

    private String title;

    private Integer level;

    /**
     * {@link com.laby.module.legal.enums.clause.LegalClauseTypeEnum}
     */
    private String type;

    private String path;

    /**
     * JSON 数组，如 ["p-1","p-2"]
     */
    private String paragraphIds;

    private String fullText;

}
