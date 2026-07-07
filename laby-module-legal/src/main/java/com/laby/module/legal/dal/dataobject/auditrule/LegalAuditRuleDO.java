package com.laby.module.legal.dal.dataobject.auditrule;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务全局审核规则 DO
 */
@TableName("legal_audit_rule")
@KeySequence("legal_audit_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAuditRuleDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 规则名称
     */
    private String name;
    /**
     * 适用合同类型，空=全部
     */
    private Long contractTypeId;
    /**
     * 条款分类
     */
    private String clauseType;
    /**
     * 关联标准条款编号
     */
    private Long standardClauseId;
    /**
     * 自定义规则正文
     */
    private String ruleContent;
    /**
     * 优先级
     */
    private Integer priority;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 说明
     */
    private String description;
    /**
     * 可执行规则类型
     */
    private String ruleType;
    /**
     * 匹配关键词或正则
     */
    private String matchPattern;
    /**
     * 匹配方式 KEYWORD / REGEX / SEMANTIC
     */
    private String matchType;
    /**
     * 命中或缺失时的风险等级
     */
    private String riskLevel;
    /**
     * 命中后动作 OPINION / WARN
     */
    private String actionOnHit;
    /**
     * Playbook 版本
     */
    private Integer playbookVersion;

}
