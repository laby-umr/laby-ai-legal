package com.laby.module.legal.service.contract.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.service.auditrule.bo.LegalAuditContextResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 RAG 批次上下文合并为 opinion.evidence_refs（设计 §13.2 / §13.4）。
 */
public final class LegalAuditEvidenceRefsBuilder {

    private static final int QUOTE_MAX = 200;

    private LegalAuditEvidenceRefsBuilder() {
    }

    public static List<Map<String, String>> buildForRule(String sourceType, String sourceId, String excerpt,
                                                         String paragraphId) {
        Map<String, String> ref = new LinkedHashMap<>();
        ref.put("sourceType", normalizeSourceType(sourceType));
        ref.put("sourceId", StrUtil.trim(sourceId));
        if (StrUtil.isNotBlank(excerpt)) {
            ref.put("excerpt", StrUtil.sub(excerpt, 0, QUOTE_MAX));
        }
        if (StrUtil.isNotBlank(paragraphId)) {
            ref.put("paragraphId", paragraphId);
        }
        return List.of(ref);
    }

    public static List<Map<String, String>> build(List<Map<String, String>> fromAi,
                                                  String sourceType,
                                                  String sourceId,
                                                  String referenceClause,
                                                  LegalAuditContextResult context) {
        Map<String, Map<String, String>> dedup = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(fromAi)) {
            for (Map<String, String> ref : fromAi) {
                putRef(dedup, ref);
            }
        }
        String normalizedType = normalizeSourceType(sourceType);
        String normalizedId = StrUtil.trim(sourceId);
        if (StrUtil.isNotBlank(normalizedId)
                && !LegalOpinionSourceTypeEnum.AI.getCode().equals(normalizedType)
                && !(LegalOpinionSourceTypeEnum.KNOWLEDGE.getCode().equals(normalizedType)
                && !isNumericSegmentId(normalizedId))) {
            Map<String, String> primary = new LinkedHashMap<>();
            primary.put("sourceType", normalizedType);
            primary.put("sourceId", normalizedId);
            if (StrUtil.isNotBlank(referenceClause)) {
                primary.put("excerpt", StrUtil.sub(referenceClause, 0, 200));
            }
            putRef(dedup, primary);
        }
        if (context != null) {
            mergeRuleRefs(dedup, normalizedType, normalizedId, context);
            mergeKnowledgeRefs(dedup, normalizedType, normalizedId, context);
        }
        return new ArrayList<>(dedup.values());
    }

    private static void mergeRuleRefs(Map<String, Map<String, String>> dedup,
                                      String sourceType, String sourceId,
                                      LegalAuditContextResult context) {
        if (CollUtil.isEmpty(context.getRules())) {
            return;
        }
        for (LegalAuditContextResult.RuleRef rule : context.getRules()) {
            if (LegalOpinionSourceTypeEnum.RULE.getCode().equals(sourceType)
                    && rule.getRuleId() != null
                    && String.valueOf(rule.getRuleId()).equals(sourceId)) {
                Map<String, String> ref = new LinkedHashMap<>();
                ref.put("sourceType", LegalOpinionSourceTypeEnum.RULE.getCode());
                ref.put("sourceId", String.valueOf(rule.getRuleId()));
                ref.put("excerpt", StrUtil.sub(StrUtil.blankToDefault(rule.getName(), ""), 0, 200));
                putRef(dedup, ref);
            }
            if (LegalOpinionSourceTypeEnum.STANDARD_CLAUSE.getCode().equals(sourceType)
                    && rule.getStandardClauseId() != null
                    && String.valueOf(rule.getStandardClauseId()).equals(sourceId)) {
                Map<String, String> ref = new LinkedHashMap<>();
                ref.put("sourceType", LegalOpinionSourceTypeEnum.STANDARD_CLAUSE.getCode());
                ref.put("sourceId", String.valueOf(rule.getStandardClauseId()));
                ref.put("excerpt", StrUtil.sub(StrUtil.blankToDefault(rule.getName(), ""), 0, 200));
                putRef(dedup, ref);
            }
        }
    }

    private static void mergeKnowledgeRefs(Map<String, Map<String, String>> dedup,
                                           String sourceType, String sourceId,
                                           LegalAuditContextResult context) {
        if (CollUtil.isEmpty(context.getKnowledgeSegments())) {
            return;
        }
        int added = 0;
        for (LegalAuditContextResult.KnowledgeRef knowledge : context.getKnowledgeSegments()) {
            if (knowledge.getSegmentId() == null) {
                continue;
            }
            String segmentKey = String.valueOf(knowledge.getSegmentId());
            boolean invalidKnowledgeId = LegalOpinionSourceTypeEnum.KNOWLEDGE.getCode().equals(sourceType)
                    && !isNumericSegmentId(sourceId);
            boolean match = LegalOpinionSourceTypeEnum.KNOWLEDGE.getCode().equals(sourceType)
                    && segmentKey.equals(sourceId);
            boolean fallbackAi = LegalOpinionSourceTypeEnum.AI.getCode().equals(sourceType) && added == 0;
            boolean fallbackInvalidKnowledge = invalidKnowledgeId && added == 0;
            if (!match && !fallbackAi && !fallbackInvalidKnowledge) {
                continue;
            }
            Map<String, String> ref = new LinkedHashMap<>();
            ref.put("sourceType", LegalOpinionSourceTypeEnum.KNOWLEDGE.getCode());
            ref.put("sourceId", segmentKey);
            if (knowledge.getDocumentId() != null) {
                ref.put("documentId", String.valueOf(knowledge.getDocumentId()));
            }
            ref.put("excerpt", StrUtil.sub(StrUtil.blankToDefault(knowledge.getExcerpt(), ""), 0, 200));
            putRef(dedup, ref);
            added++;
            if (fallbackAi && added >= 1) {
                break;
            }
            if (fallbackInvalidKnowledge && added >= 1) {
                break;
            }
            if (match) {
                break;
            }
        }
    }

    private static boolean isNumericSegmentId(String sourceId) {
        if (StrUtil.isBlank(sourceId)) {
            return false;
        }
        for (int i = 0; i < sourceId.length(); i++) {
            if (!Character.isDigit(sourceId.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static void putRef(Map<String, Map<String, String>> dedup, Map<String, String> ref) {
        if (ref == null || ref.isEmpty()) {
            return;
        }
        String key = StrUtil.blankToDefault(ref.get("sourceType"), "") + "#"
                + StrUtil.blankToDefault(ref.get("sourceId"), "") + "#"
                + StrUtil.blankToDefault(ref.get("documentId"), "");
        dedup.putIfAbsent(key, ref);
    }

    private static String normalizeSourceType(String sourceType) {
        if (StrUtil.isBlank(sourceType)) {
            return LegalOpinionSourceTypeEnum.AI.getCode();
        }
        for (LegalOpinionSourceTypeEnum item : LegalOpinionSourceTypeEnum.values()) {
            if (item.getCode().equalsIgnoreCase(sourceType)) {
                return item.getCode();
            }
        }
        return LegalOpinionSourceTypeEnum.AI.getCode();
    }

}
