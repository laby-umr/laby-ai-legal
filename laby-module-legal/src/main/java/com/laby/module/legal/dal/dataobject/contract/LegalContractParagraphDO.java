package com.laby.module.legal.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务合同段落 DO
 */
@TableName("legal_contract_paragraph")
@KeySequence("legal_contract_paragraph_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractParagraphDO extends TenantBaseDO {

    /**
     * 段落编号
     */
    @TableId
    private Long id;
    /**
     * 合同编号
     */
    private Long contractId;
    /**
     * 段落业务 ID，如 p-1
     */
    private String paragraphId;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 段落文本
     */
    private String text;
    /**
     * 层级路径
     */
    private String path;
    /**
     * 标记后 AI 审核跳过本段
     */
    private Boolean skipAudit;

}
