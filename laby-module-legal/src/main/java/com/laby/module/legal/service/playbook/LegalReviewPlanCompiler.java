package com.laby.module.legal.service.playbook;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.auditrule.LegalAuditRuleDO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import com.laby.module.legal.dal.mysql.auditrule.LegalAuditRuleMapper;
import com.laby.module.legal.enums.auditrule.LegalAuditRuleTypeEnum;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanRuleBO;
import com.laby.module.legal.service.standardclause.LegalStandardClauseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将启用规则编译为 ReviewPlan
 */
@Component
public class LegalReviewPlanCompiler {

    @Resource
    private LegalAuditRuleMapper auditRuleMapper;
    @Resource
    private LegalStandardClauseService standardClauseService;

    public LegalReviewPlanBO compile(Long contractTypeId) {
        List<LegalAuditRuleDO> rules = auditRuleMapper.selectEnabledForAudit(contractTypeId);
        if (CollUtil.isEmpty(rules)) {
            return LegalReviewPlanBO.builder()
                    .contractTypeId(contractTypeId)
                    .version(1)
                    .build();
        }
        Set<Long> clauseIds = rules.stream()
                .map(LegalAuditRuleDO::getStandardClauseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, LegalStandardClauseDO> clauseMap = standardClauseService.getStandardClauseMap(clauseIds);

        List<LegalReviewPlanRuleBO> deterministic = new ArrayList<>();
        List<LegalReviewPlanRuleBO> llm = new ArrayList<>();
        int maxVersion = 1;
        for (LegalAuditRuleDO rule : rules) {
            if (rule.getPlaybookVersion() != null) {
                maxVersion = Math.max(maxVersion, rule.getPlaybookVersion());
            }
            LegalReviewPlanRuleBO planRule = toPlanRule(rule, clauseMap);
            LegalAuditRuleTypeEnum type = LegalAuditRuleTypeEnum.of(rule.getRuleType());
            if (type == LegalAuditRuleTypeEnum.CUSTOM_LLM) {
                llm.add(planRule);
            } else {
                deterministic.add(planRule);
            }
        }
        return LegalReviewPlanBO.builder()
                .contractTypeId(contractTypeId)
                .version(maxVersion)
                .deterministicRules(deterministic)
                .llmRules(llm)
                .build();
    }

    private static LegalReviewPlanRuleBO toPlanRule(LegalAuditRuleDO rule,
                                                    Map<Long, LegalStandardClauseDO> clauseMap) {
        LegalStandardClauseDO standardClause = rule.getStandardClauseId() != null
                ? clauseMap.get(rule.getStandardClauseId()) : null;
        return LegalReviewPlanRuleBO.builder()
                .ruleId(rule.getId())
                .name(rule.getName())
                .ruleType(LegalAuditRuleTypeEnum.of(rule.getRuleType()).getCode())
                .matchPattern(rule.getMatchPattern())
                .matchType(rule.getMatchType())
                .riskLevel(StrUtil.blankToDefault(rule.getRiskLevel(), "HIGH"))
                .actionOnHit(StrUtil.blankToDefault(rule.getActionOnHit(), "OPINION"))
                .clauseType(rule.getClauseType())
                .standardClauseId(rule.getStandardClauseId())
                .standardClauseName(standardClause != null ? standardClause.getName() : null)
                .ruleContent(rule.getRuleContent())
                .priority(rule.getPriority())
                .build();
    }

}
