package com.laby.module.legal.service.playbook.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Playbook 编译后的单条可执行规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalReviewPlanRuleBO {

    private Long ruleId;
    private String name;
    private String ruleType;
    private String matchPattern;
    private String matchType;
    private String riskLevel;
    private String actionOnHit;
    private String clauseType;
    private Long standardClauseId;
    private String standardClauseName;
    private String ruleContent;
    private Integer priority;

}
