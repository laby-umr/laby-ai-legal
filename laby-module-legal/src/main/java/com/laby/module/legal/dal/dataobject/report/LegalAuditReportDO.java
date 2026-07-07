package com.laby.module.legal.dal.dataobject.report;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务审核报告 DO
 */
@TableName("legal_audit_report")
@KeySequence("legal_audit_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAuditReportDO extends TenantBaseDO {

    /**
     * 报告编号
     */
    @TableId
    private Long id;
    /**
     * 合同编号
     */
    private Long contractId;
    /**
     * 审核轮次
     */
    private Integer auditRound;
    /**
     * Markdown 报告内容
     */
    private String content;

}
