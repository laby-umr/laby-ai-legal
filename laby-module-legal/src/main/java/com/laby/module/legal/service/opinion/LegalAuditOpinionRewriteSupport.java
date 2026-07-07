package com.laby.module.legal.service.opinion;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 审核意见「可执行改写」归一化：区分给人看的 suggestion 与写入合同的 newText。
 */
public final class LegalAuditOpinionRewriteSupport {

    private static final Pattern SUGGESTION_REWRITE_PREFIX = Pattern.compile(
            "^(?:建议)?(?:修改(?:为|成)|改(?:为|成)|修订(?:为|成)|调整为?)[：:\\s]+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern MISSING_CLAUSE_INTENT = Pattern.compile(
            "缺少|缺失|未约定|未明确|未规定|未载明|未设置|未列明|应补充|建议补充|补充约定|补充相关|补充.*条款|缺少.*条款",
            Pattern.CASE_INSENSITIVE);

    private static final int MIN_REFERENCE_CLAUSE_CHARS = 24;

    private LegalAuditOpinionRewriteSupport() {
    }

    /**
     * 归一化 LLM 批审输出：补全 changeType / newText，避免仅有「建议」文案。
     */
    public static void normalizeAiOpinionItem(LegalAiAuditOpinionItemBO item, String paragraphText) {
        if (item == null) {
            return;
        }
        item.setSuggestion(StrUtil.trim(item.getSuggestion()));
        item.setOldText(StrUtil.trim(item.getOldText()));
        item.setNewText(cleanRewriteText(item.getNewText()));

        if (StrUtil.isBlank(item.getNewText()) && StrUtil.isNotBlank(item.getSuggestion())) {
            item.setNewText(extractRewriteTextFromSuggestion(item.getSuggestion()));
        }

        String changeType = normalizeChangeTypeCode(item.getChangeType());
        if (StrUtil.isNotBlank(item.getNewText())) {
            if (LegalOpinionChangeTypeEnum.NO_CHANGE.getCode().equals(changeType)) {
                item.setChangeType(LegalOpinionChangeTypeEnum.REPLACE.getCode());
            }
            if (StrUtil.isBlank(item.getOldText()) && StrUtil.isNotBlank(paragraphText)) {
                item.setOldText(inferOldTextFromParagraph(paragraphText, item.getNewText()));
            }
            return;
        }
        if (LegalOpinionChangeTypeEnum.DELETE.getCode().equals(changeType)
                && StrUtil.isNotBlank(item.getOldText())) {
            return;
        }
        applyMissingClauseDefaults(item);
    }

    public static boolean needsClauseDraft(LegalAiAuditOpinionItemBO item) {
        return item != null
                && isMissingClauseIntent(item)
                && StrUtil.isBlank(item.getNewText())
                && StrUtil.isNotBlank(item.getParagraphId());
    }

    public static boolean isMissingClauseIntent(LegalAiAuditOpinionItemBO item) {
        if (item == null) {
            return false;
        }
        return isMissingClauseText(joinIntentText(
                item.getTitle(), item.getContent(), item.getSuggestion()));
    }

    public static boolean isMissingClauseOpinion(LegalAuditOpinionDO opinion) {
        if (opinion == null) {
            return false;
        }
        return isMissingClauseText(joinIntentText(
                opinion.getTitle(), opinion.getContent(), opinion.getSuggestion()));
    }

    private static void applyMissingClauseDefaults(LegalAiAuditOpinionItemBO item) {
        if (!isMissingClauseIntent(item)) {
            item.setChangeType(LegalOpinionChangeTypeEnum.NO_CHANGE.getCode());
            return;
        }
        if (StrUtil.isNotBlank(item.getNewText())) {
            if (StrUtil.isBlank(item.getOldText())) {
                item.setChangeType(resolveInsertChangeType(item.getChangeType()));
            }
            return;
        }
        String referenceClause = StrUtil.trim(item.getReferenceClause());
        if (StrUtil.length(referenceClause) >= MIN_REFERENCE_CLAUSE_CHARS) {
            item.setNewText(cleanRewriteText(referenceClause));
            item.setChangeType(resolveInsertChangeType(item.getChangeType()));
        }
    }

    private static String resolveInsertChangeType(String changeType) {
        String normalized = normalizeChangeTypeCode(changeType);
        if (LegalOpinionChangeTypeEnum.INSERT_BEFORE.getCode().equals(normalized)) {
            return normalized;
        }
        return LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode();
    }

    private static boolean isMissingClauseText(String text) {
        return StrUtil.isNotBlank(text) && MISSING_CLAUSE_INTENT.matcher(text).find();
    }

    private static String joinIntentText(String... parts) {
        StringBuilder builder = new StringBuilder();
        if (parts != null) {
            for (String part : parts) {
                if (StrUtil.isNotBlank(part)) {
                    if (builder.length() > 0) {
                        builder.append(' ');
                    }
                    builder.append(part.trim());
                }
            }
        }
        return builder.toString();
    }

    /**
     * 是否可将采纳结果写入 WORKING 正文（必须有可执行的 oldText/newText 或插入/删除片段）。
     */
    public static boolean isAdoptApplicableToDocument(LegalAuditOpinionDO opinion) {
        if (opinion == null) {
            return false;
        }
        String changeType = normalizeChangeTypeCode(opinion.getChangeType());
        String oldText = StrUtil.trim(opinion.getOldText());
        String newText = StrUtil.trim(opinion.getNewText());

        if (LegalOpinionChangeTypeEnum.DELETE.getCode().equals(changeType)) {
            return StrUtil.isNotBlank(oldText);
        }
        if (LegalOpinionChangeTypeEnum.INSERT_BEFORE.getCode().equals(changeType)
                || LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode().equals(changeType)) {
            return StrUtil.isNotBlank(newText);
        }
        if (LegalOpinionChangeTypeEnum.REPLACE.getCode().equals(changeType)) {
            return StrUtil.isNotBlank(newText);
        }
        // NO_CHANGE：仅当提供了 newText（局部或整段替换）才可写正文，不再使用 suggestion
        return LegalOpinionChangeTypeEnum.NO_CHANGE.getCode().equals(changeType)
                && StrUtil.isNotBlank(newText);
    }

    public static String normalizeChangeTypeCode(String changeType) {
        if (StrUtil.isBlank(changeType)) {
            return LegalOpinionChangeTypeEnum.NO_CHANGE.getCode();
        }
        for (LegalOpinionChangeTypeEnum item : LegalOpinionChangeTypeEnum.values()) {
            if (item.getCode().equalsIgnoreCase(changeType.trim())) {
                return item.getCode();
            }
        }
        return LegalOpinionChangeTypeEnum.NO_CHANGE.getCode();
    }

    static String extractRewriteTextFromSuggestion(String suggestion) {
        if (StrUtil.isBlank(suggestion)) {
            return "";
        }
        String trimmed = suggestion.trim();
        Matcher matcher = SUGGESTION_REWRITE_PREFIX.matcher(trimmed);
        if (matcher.matches()) {
            return cleanRewriteText(matcher.group(1));
        }
        if (trimmed.startsWith("建议") && trimmed.length() < 80) {
            return "";
        }
        return "";
    }

    static String cleanRewriteText(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("建议：") || cleaned.startsWith("建议:")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.startsWith("建议")) {
            Matcher matcher = SUGGESTION_REWRITE_PREFIX.matcher(cleaned);
            if (matcher.matches()) {
                cleaned = matcher.group(1).trim();
            }
        }
        return cleaned;
    }

    private static String inferOldTextFromParagraph(String paragraphText, String newText) {
        if (StrUtil.isBlank(paragraphText) || StrUtil.isBlank(newText)) {
            return "";
        }
        String paragraph = paragraphText.trim();
        if (paragraph.equals(newText.trim())) {
            return paragraph;
        }
        return "";
    }

}
