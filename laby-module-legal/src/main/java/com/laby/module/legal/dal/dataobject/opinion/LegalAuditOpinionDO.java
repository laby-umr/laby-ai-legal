package com.laby.module.legal.dal.dataobject.opinion;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务审核意见 DO
 */
@TableName("legal_audit_opinion")
@KeySequence("legal_audit_opinion_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAuditOpinionDO extends TenantBaseDO {

    /**
     * 意见编号
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
     * 条款类型
     */
    private String clauseType;
    /**
     * 风险等级 HIGH/MEDIUM/LOW
     */
    private String riskLevel;
    /**
     * 标题
     */
    private String title;
    /**
     * 意见内容
     */
    private String content;
    /**
     * 修改建议
     */
    private String suggestion;
    /**
     * 段落定位
     */
    private String paragraphId;
    /**
     * 条款定位，如 c-1
     */
    private String clauseId;
    /**
     * 对照的标准条款摘要
     */
    private String referenceClause;
    /**
     * 意见来源类型：AI/RULE/STANDARD_CLAUSE/MANUAL
     */
    private String sourceType;
    /**
     * 意见来源主键，如规则ID/条款ID
     */
    private String sourceId;
    /**
     * 来源版本号或发布时间戳
     */
    private String sourceVersion;
    /**
     * 该意见对应的合同版本ID
     */
    private Long fromVersionId;
    /**
     * 改写类型：REPLACE/INSERT_BEFORE/INSERT_AFTER/DELETE/NO_CHANGE
     */
    private String changeType;
    /**
     * 改写前文本（用于冲突校验）
     */
    private String oldText;
    /**
     * 改写后文本（用于导出新合同）
     */
    private String newText;
    /**
     * 结构化证据引用 JSON 数组
     */
    private String evidenceRefs;
    /**
     * 0 待处置 1 采纳 2 忽略
     */
    private Integer status;

}
