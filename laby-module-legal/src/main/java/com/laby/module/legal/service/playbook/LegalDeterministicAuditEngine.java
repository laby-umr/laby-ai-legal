package com.laby.module.legal.service.playbook;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import com.laby.module.legal.enums.auditrule.LegalAuditMatchTypeEnum;
import com.laby.module.legal.enums.auditrule.LegalAuditRuleTypeEnum;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import com.laby.module.legal.service.contract.util.LegalAuditEvidenceRefsBuilder;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanRuleBO;
import com.laby.module.legal.service.standardclause.LegalStandardClauseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Playbook 确定性审阅引擎（不调用 LLM）
 */
@Slf4j
@Component
public class LegalDeterministicAuditEngine {

    private static final int QUOTE_MAX = 200;

    @Resource
    private LegalStandardClauseService standardClauseService;

    public List<LegalAuditOpinionDraftBO> run(LegalReviewPlanBO plan,
                                              List<LegalContractClauseDO> clauses,
                                              List<LegalContractParagraphDO> paragraphs) {
        if (plan == null || CollUtil.isEmpty(plan.getDeterministicRules())) {
            return List.of();
        }
        List<LegalAuditOpinionDraftBO> drafts = new ArrayList<>();
        Set<String> dedup = new LinkedHashSet<>();
        for (LegalReviewPlanRuleBO rule : plan.getDeterministicRules()) {
            LegalAuditRuleTypeEnum type = LegalAuditRuleTypeEnum.of(rule.getRuleType());
            switch (type) {
                case MANDATORY_CLAUSE -> drafts.addAll(checkMandatory(rule, clauses, paragraphs, dedup));
                case FORBIDDEN_PATTERN -> drafts.addAll(checkForbidden(rule, clauses, paragraphs, dedup));
                case PREFERRED_CLAUSE -> drafts.addAll(checkPreferred(rule, clauses, paragraphs, dedup));
                default -> { /* CUSTOM_LLM 跳过 */ }
            }
        }
        return drafts;
    }

    private List<LegalAuditOpinionDraftBO> checkMandatory(LegalReviewPlanRuleBO rule,
                                                          List<LegalContractClauseDO> clauses,
                                                          List<LegalContractParagraphDO> paragraphs,
                                                          Set<String> dedup) {
        String keyword = resolveKeyword(rule);
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }
        if (containsKeyword(clauses, paragraphs, keyword)) {
            return List.of();
        }
        String newText = resolveRuleStandardClauseText(rule);
        String changeType = StrUtil.isNotBlank(newText)
                ? LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode()
                : LegalOpinionChangeTypeEnum.NO_CHANGE.getCode();
        return List.of(singleDraft(rule, dedup,
                "缺少必备条款：" + rule.getName(),
                "合同中未检测到必备内容「" + keyword + "」。",
                StrUtil.isNotBlank(newText)
                        ? "建议补充标准条款：" + StrUtil.blankToDefault(rule.getStandardClauseName(), rule.getName())
                        : "建议补充相关条款或引用标准条款。",
                resolveInsertAnchorParagraphId(paragraphs), null, null,
                LegalOpinionSourceTypeEnum.RULE.getCode(), String.valueOf(rule.getRuleId()),
                changeType, newText));
    }

    private List<LegalAuditOpinionDraftBO> checkForbidden(LegalReviewPlanRuleBO rule,
                                                          List<LegalContractClauseDO> clauses,
                                                          List<LegalContractParagraphDO> paragraphs,
                                                          Set<String> dedup) {
        Pattern pattern = compilePattern(rule);
        if (pattern == null) {
            return List.of();
        }
        List<LegalAuditOpinionDraftBO> drafts = new ArrayList<>();
        if (CollUtil.isNotEmpty(clauses)) {
            for (LegalContractClauseDO clause : clauses) {
                collectForbiddenHits(rule, pattern, clause.getFullText(), clause.getClauseId(),
                        firstParagraphId(clause), dedup, drafts);
            }
        }
        if (CollUtil.isNotEmpty(paragraphs)) {
            for (LegalContractParagraphDO paragraph : paragraphs) {
                collectForbiddenHits(rule, pattern, paragraph.getText(), null,
                        paragraph.getParagraphId(), dedup, drafts);
            }
        }
        return drafts;
    }

    private List<LegalAuditOpinionDraftBO> checkPreferred(LegalReviewPlanRuleBO rule,
                                                          List<LegalContractClauseDO> clauses,
                                                          List<LegalContractParagraphDO> paragraphs,
                                                          Set<String> dedup) {
        if (rule.getStandardClauseId() == null) {
            return List.of();
        }
        LegalStandardClauseDO standardClause = standardClauseService.getStandardClause(rule.getStandardClauseId());
        if (standardClause == null) {
            return List.of();
        }
        String keyword = StrUtil.blankToDefault(rule.getMatchPattern(), standardClause.getName());
        if (containsKeyword(clauses, paragraphs, keyword)) {
            return List.of();
        }
        String clauseText = StrUtil.trim(standardClause.getContent());
        return List.of(singleDraft(rule, dedup,
                "偏离推荐条款：" + standardClause.getName(),
                "未检测到与标准条款「" + standardClause.getName() + "」相符的表述（关键词：" + keyword + "）。",
                "建议补充标准条款：" + standardClause.getName(),
                resolveInsertAnchorParagraphId(paragraphs), null, null,
                LegalOpinionSourceTypeEnum.STANDARD_CLAUSE.getCode(),
                String.valueOf(standardClause.getId()),
                LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode(),
                clauseText));
    }

    private void collectForbiddenHits(LegalReviewPlanRuleBO rule, Pattern pattern, String text,
                                      String clauseId, String paragraphId,
                                      Set<String> dedup, List<LegalAuditOpinionDraftBO> drafts) {
        if (StrUtil.isBlank(text)) {
            return;
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return;
        }
        String quote = StrUtil.sub(matcher.group(), 0, QUOTE_MAX);
        LegalAuditOpinionDraftBO draft = singleDraft(rule, dedup,
                "禁止表述：" + rule.getName(),
                "检测到可能违规表述：「" + quote + "」",
                StrUtil.blankToDefault(rule.getRuleContent(), "建议修改或删除该表述。"),
                paragraphId, clauseId, quote);
        if (draft != null) {
            drafts.add(draft);
        }
    }

    private LegalAuditOpinionDraftBO singleDraft(LegalReviewPlanRuleBO rule, Set<String> dedup,
                                                 String title, String content, String suggestion,
                                                 String paragraphId, String clauseId, String quote) {
        return singleDraft(rule, dedup, title, content, suggestion, paragraphId, clauseId, quote,
                LegalOpinionSourceTypeEnum.RULE.getCode(), String.valueOf(rule.getRuleId()),
                LegalOpinionChangeTypeEnum.NO_CHANGE.getCode(), null);
    }

    private LegalAuditOpinionDraftBO singleDraft(LegalReviewPlanRuleBO rule, Set<String> dedup,
                                                 String title, String content, String suggestion,
                                                 String paragraphId, String clauseId, String quote,
                                                 String sourceType, String sourceId,
                                                 String changeType, String newText) {
        String key = rule.getRuleId() + "#" + StrUtil.blankToDefault(paragraphId, "") + "#" + title;
        if (!dedup.add(key)) {
            return null;
        }
        String risk = normalizeRisk(rule.getRiskLevel());
        List<Map<String, String>> evidenceRefs = null;
        if (StrUtil.isNotBlank(quote)) {
            evidenceRefs = LegalAuditEvidenceRefsBuilder.buildForRule(
                    sourceType, sourceId, quote, paragraphId);
        }
        return LegalAuditOpinionDraftBO.builder()
                .clauseType(rule.getClauseType())
                .riskLevel(risk)
                .title(title)
                .content(content)
                .suggestion(suggestion)
                .paragraphId(paragraphId)
                .clauseId(clauseId)
                .referenceClause(rule.getStandardClauseName())
                .sourceType(sourceType)
                .sourceId(sourceId)
                .changeType(StrUtil.blankToDefault(changeType, LegalOpinionChangeTypeEnum.NO_CHANGE.getCode()))
                .oldText(quote)
                .newText(newText)
                .evidenceRefs(evidenceRefs)
                .build();
    }

    private String resolveRuleStandardClauseText(LegalReviewPlanRuleBO rule) {
        if (rule == null || rule.getStandardClauseId() == null) {
            return null;
        }
        LegalStandardClauseDO standardClause = standardClauseService.getStandardClause(rule.getStandardClauseId());
        return standardClause == null ? null : StrUtil.trim(standardClause.getContent());
    }

    private static String resolveInsertAnchorParagraphId(List<LegalContractParagraphDO> paragraphs) {
        if (CollUtil.isEmpty(paragraphs)) {
            return null;
        }
        for (int i = paragraphs.size() - 1; i >= 0; i--) {
            LegalContractParagraphDO paragraph = paragraphs.get(i);
            if (paragraph != null && StrUtil.isNotBlank(paragraph.getParagraphId())) {
                return paragraph.getParagraphId();
            }
        }
        return null;
    }

    private static boolean containsKeyword(List<LegalContractClauseDO> clauses,
                                           List<LegalContractParagraphDO> paragraphs,
                                           String keyword) {
        if (CollUtil.isNotEmpty(clauses)) {
            for (LegalContractClauseDO clause : clauses) {
                if (StrUtil.containsIgnoreCase(clause.getFullText(), keyword)
                        || StrUtil.containsIgnoreCase(clause.getTitle(), keyword)) {
                    return true;
                }
            }
        }
        if (CollUtil.isNotEmpty(paragraphs)) {
            for (LegalContractParagraphDO paragraph : paragraphs) {
                if (StrUtil.containsIgnoreCase(paragraph.getText(), keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String resolveKeyword(LegalReviewPlanRuleBO rule) {
        if (StrUtil.isNotBlank(rule.getMatchPattern())) {
            return rule.getMatchPattern().trim();
        }
        if (StrUtil.isNotBlank(rule.getClauseType())) {
            return rule.getClauseType().trim();
        }
        return StrUtil.blankToDefault(rule.getName(), "").trim();
    }

    private static Pattern compilePattern(LegalReviewPlanRuleBO rule) {
        String patternText = StrUtil.blankToDefault(rule.getMatchPattern(), rule.getRuleContent());
        if (StrUtil.isBlank(patternText)) {
            return null;
        }
        LegalAuditMatchTypeEnum matchType = LegalAuditMatchTypeEnum.of(rule.getMatchType());
        try {
            if (matchType == LegalAuditMatchTypeEnum.REGEX) {
                return Pattern.compile(patternText, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            }
            return Pattern.compile(Pattern.quote(patternText.trim()), Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            log.warn("[compilePattern][ruleId={}] 正则无效: {}", rule.getRuleId(), ex.getMessage());
            return null;
        }
    }

    private static String firstParagraphId(LegalContractClauseDO clause) {
        List<String> ids = JsonUtils.parseArray(clause.getParagraphIds(), String.class);
        return CollUtil.isNotEmpty(ids) ? ids.get(0) : null;
    }

    private static String normalizeRisk(String riskLevel) {
        if (LegalRiskLevelEnum.MEDIUM.getCode().equalsIgnoreCase(riskLevel)) {
            return LegalRiskLevelEnum.MEDIUM.getCode();
        }
        if (LegalRiskLevelEnum.LOW.getCode().equalsIgnoreCase(riskLevel)) {
            return LegalRiskLevelEnum.LOW.getCode();
        }
        return LegalRiskLevelEnum.HIGH.getCode();
    }

}
