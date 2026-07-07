package com.laby.module.legal.service.report;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.enums.contract.LegalComplianceResultEnum;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按 PRD 样例生成六章《合同审核报告》Markdown。
 */
public final class LegalAuditReportBuilder {

    private static final DateTimeFormatter AUDIT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LegalAuditReportBuilder() {
    }

    public static String build(LegalContractDO contract, int auditRound,
                               List<OpinionView> items, LocalDateTime auditTime) {
        LocalDateTime time = auditTime != null ? auditTime : LocalDateTime.now();
        List<OpinionView> opinions = CollUtil.isEmpty(items) ? List.of() : items;
        StringBuilder sb = new StringBuilder(4096);

        appendHeader(sb, contract, auditRound, time);
        appendChapter1(sb, contract, auditRound, opinions);
        appendChapter2(sb, opinions);
        appendChapter3(sb, opinions);
        appendChapter4(sb, opinions);
        appendChapter5(sb, opinions);
        appendChapter6(sb, contract, auditRound, opinions);
        return sb.toString();
    }

    private static void appendHeader(StringBuilder sb, LegalContractDO contract,
                                     int auditRound, LocalDateTime auditTime) {
        sb.append("# 合同审核报告\n\n");
        sb.append("| 字段 | 内容 |\n");
        sb.append("|------|------|\n");
        sb.append("| 合同名称 | ").append(escapeCell(contract.getTitle())).append(" |\n");
        sb.append("| 合同编号 | LEGAL-").append(contract.getId()).append(" |\n");
        sb.append("| 审核时间 | ").append(auditTime.format(AUDIT_TIME_FORMAT)).append(" |\n");
        sb.append("| 审核人员 | AI 助手 |\n");
        sb.append("| 审核轮次 | 第 ").append(auditRound).append(" 轮 |\n");
        sb.append("| 我方立场 | ").append(escapeCell(StrUtil.blankToDefault(contract.getPartyRole(), "—"))).append(" |\n");
        sb.append("| 审核强度 | ").append(escapeCell(StrUtil.blankToDefault(contract.getAuditLevel(), "—"))).append(" |\n\n");
        sb.append("> 本报告由 AI 自动生成，用于识别合同风险并给出专业建议，供法务人员参考。\n\n");
    }

    private static void appendChapter1(StringBuilder sb, LegalContractDO contract,
                                         int auditRound, List<OpinionView> opinions) {
        sb.append("## 一、合同摘要\n\n");
        sb.append("### 1.1 合同背景及交易背景\n\n");
        sb.append("本报告针对《").append(StrUtil.blankToDefault(contract.getTitle(), "未命名合同"))
                .append("》进行第 ").append(auditRound).append(" 轮 AI 审核。");
        sb.append("我方立场为 **").append(StrUtil.blankToDefault(contract.getPartyRole(), "未指定")).append("**。");
        if (CollUtil.isEmpty(opinions)) {
            sb.append("本轮未识别到需提示的结构化风险意见。\n\n");
        } else {
            sb.append("共识别 **").append(opinions.size()).append("** 条审核意见，");
            sb.append("其中高风险 **").append(countByLevel(opinions, LegalRiskLevelEnum.HIGH)).append("** 条、");
            sb.append("中风险 **").append(countByLevel(opinions, LegalRiskLevelEnum.MEDIUM)).append("** 条、");
            sb.append("低风险 **").append(countByLevel(opinions, LegalRiskLevelEnum.LOW)).append("** 条。\n\n");
        }

        sb.append("### 1.2 交易对价及支付\n\n");
        String paymentHint = extractThematicHint(opinions, "价款", "支付", "付款", "对价", "费用", "价格");
        sb.append(StrUtil.isNotBlank(paymentHint) ? paymentHint
                : "请结合合同正文核对总价、付款节点、付款方式及发票开具约定；AI 未从意见中抽取到明确的价款/支付摘要。\n\n");

        sb.append("### 1.3 合同有效期\n\n");
        String validityHint = extractThematicHint(opinions, "有效期", "期限", "生效", "到期", "终止", "续期");
        sb.append(StrUtil.isNotBlank(validityHint) ? validityHint
                : "请结合合同正文核对生效日、到期日、续期或终止通知期；AI 未从意见中抽取到明确的有效期摘要。\n\n");
    }

    private static void appendChapter2(StringBuilder sb, List<OpinionView> opinions) {
        sb.append("## 二、风险点\n\n");
        if (CollUtil.isEmpty(opinions)) {
            sb.append("本轮审核未发现需特别提示的风险点。\n\n");
            return;
        }
        appendRiskNarrative(sb, "付款风险", opinions, "价款", "支付", "付款", "对价", "费用", "价格", "账期");
        appendRiskNarrative(sb, "交付风险", opinions, "交付", "验收", "工期", "服务", "交付物");
        appendRiskNarrative(sb, "违约责任", opinions, "违约", "责任", "赔偿", "罚金", "违约金");
        appendRiskNarrative(sb, "知识产权", opinions, "知产", "知识产权", "专利", "著作权", "商标");
        appendRiskNarrative(sb, "争议解决", opinions, "争议", "仲裁", "管辖", "适用法律", "诉讼");
        List<OpinionView> others = opinions.stream()
                .filter(o -> !matchesAnyTheme(o, "价款", "支付", "付款", "对价", "费用", "价格", "账期",
                        "交付", "验收", "工期", "服务", "交付物",
                        "违约", "责任", "赔偿", "罚金", "违约金",
                        "知产", "知识产权", "专利", "著作权", "商标",
                        "争议", "仲裁", "管辖", "适用法律", "诉讼"))
                .collect(Collectors.toList());
        appendRiskNarrative(sb, "其他风险", others);
    }

    private static void appendRiskNarrative(StringBuilder sb, String theme,
                                            List<OpinionView> opinions, String... keywords) {
        List<OpinionView> matched = opinions.stream()
                .filter(o -> matchesAnyTheme(o, keywords))
                .collect(Collectors.toList());
        appendRiskNarrative(sb, theme, matched);
    }

    private static void appendRiskNarrative(StringBuilder sb, String theme, List<OpinionView> matched) {
        if (CollUtil.isEmpty(matched)) {
            return;
        }
        sb.append("### ").append(theme).append("\n\n");
        for (OpinionView item : matched) {
            sb.append("- **").append(StrUtil.blankToDefault(item.getTitle(), "审核意见")).append("**（")
                    .append(toChineseLevel(item.getRiskLevel())).append("）：");
            sb.append(StrUtil.blankToDefault(item.getContent(), "—"));
            if (StrUtil.isNotBlank(item.getSuggestion())) {
                sb.append(" **建议措施：** ").append(item.getSuggestion());
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private static void appendChapter3(StringBuilder sb, List<OpinionView> opinions) {
        sb.append("## 三、风险分级\n\n");
        sb.append("| 风险项 | 风险描述 | 级别 | 建议 | 备注 |\n");
        sb.append("|--------|----------|------|------|------|\n");
        if (CollUtil.isEmpty(opinions)) {
            sb.append("| — | 本轮未发现结构化风险 | 低 | 可进入人工复核 | — |\n\n");
            return;
        }
        for (OpinionView item : sortByRisk(opinions)) {
            sb.append("| ").append(escapeCell(StrUtil.blankToDefault(item.getTitle(), "审核意见")))
                    .append(" | ").append(escapeCell(StrUtil.blankToDefault(item.getContent(), "—")))
                    .append(" | ").append(toChineseLevel(item.getRiskLevel()))
                    .append(" | ").append(escapeCell(StrUtil.blankToDefault(item.getSuggestion(), "—")))
                    .append(" | ").append(escapeCell(formatRemark(item)))
                    .append(" |\n");
        }
        sb.append("\n");
    }

    private static void appendChapter4(StringBuilder sb, List<OpinionView> opinions) {
        sb.append("## 四、条款不合规审查\n\n");
        sb.append("### 4.1 合规性审查\n\n");
        long high = countByLevel(opinions, LegalRiskLevelEnum.HIGH);
        long medium = countByLevel(opinions, LegalRiskLevelEnum.MEDIUM);
        if (high > 0) {
            sb.append("存在 **").append(high).append("** 项高风险意见，建议在签署前完成条款修订或取得法务书面确认。\n\n");
        } else if (medium > 0) {
            sb.append("未发现高风险项，但存在 **").append(medium).append("** 项中风险意见，建议重点复核。\n\n");
        } else if (CollUtil.isEmpty(opinions)) {
            sb.append("本轮 AI 未识别明显不合规条款，仍建议人工复核强制性法律规定与公司政策。\n\n");
        } else {
            sb.append("主要为低风险提示项，整体合规性风险可控，建议例行复核。\n\n");
        }

        sb.append("### 4.2 市场合规性\n\n");
        sb.append("请结合所属行业监管要求（如反商业贿赂、数据合规、出口管制等）进行补充审查；");
        sb.append("本报告基于合同文本与 AI 规则输出，不替代专项合规论证。\n\n");

        sb.append("### 4.3 合规条款详情\n\n");
        sb.append("| 条款名称 | 风险等级 | 状态 | 建议 |\n");
        sb.append("|----------|----------|------|------|\n");
        if (CollUtil.isEmpty(opinions)) {
            sb.append("| — | 低 | 通过 | 无 |\n\n");
            return;
        }
        Map<String, List<OpinionView>> byClause = groupByClauseType(opinions);
        for (Map.Entry<String, List<OpinionView>> entry : byClause.entrySet()) {
            String clauseName = entry.getKey();
            List<OpinionView> group = entry.getValue();
            String maxLevel = maxRiskLevel(group);
            sb.append("| ").append(escapeCell(clauseName))
                    .append(" | ").append(toChineseLevel(maxLevel))
                    .append(" | ").append(complianceStatus(maxLevel))
                    .append(" | ").append(escapeCell(summarizeSuggestions(group)))
                    .append(" |\n");
        }
        sb.append("\n");
    }

    private static void appendChapter5(StringBuilder sb, List<OpinionView> opinions) {
        sb.append("## 五、单项扣分动作\n\n");
        sb.append("| 扣分项 | 描述 | 分值 | 对应条款 |\n");
        sb.append("|--------|------|------|----------|\n");
        if (CollUtil.isEmpty(opinions)) {
            sb.append("| — | 无扣分项 | 0 | — |\n\n");
            return;
        }
        for (OpinionView item : sortByRisk(opinions)) {
            LegalRiskLevelEnum level = normalizeLevel(item.getRiskLevel());
            int score = -level.getDeductScore();
            sb.append("| ").append(escapeCell(StrUtil.blankToDefault(item.getTitle(), "审核意见")))
                    .append(" | ").append(escapeCell(StrUtil.blankToDefault(item.getContent(), "—")))
                    .append(" | ").append(score)
                    .append(" | ").append(escapeCell(formatClauseRef(item)))
                    .append(" |\n");
        }
        sb.append("\n");
    }

    private static void appendChapter6(StringBuilder sb, LegalContractDO contract,
                                       int auditRound, List<OpinionView> opinions) {
        sb.append("## 六、后续 Review 与跟进\n\n");
        if (auditRound > 1 && StrUtil.isNotBlank(contract.getFeedbackSummary())) {
            sb.append("**二轮审核说明：** ").append(contract.getFeedbackSummary()).append("\n\n");
        }
        List<OpinionView> actionable = opinions.stream()
                .filter(o -> {
                    LegalRiskLevelEnum level = normalizeLevel(o.getRiskLevel());
                    return level == LegalRiskLevelEnum.HIGH || level == LegalRiskLevelEnum.MEDIUM;
                })
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(actionable)) {
            sb.append("1. 建议法务人工通读合同全文，确认 AI 未覆盖的商业条款与附件。\n");
            sb.append("2. 若需对外发送，请按公司用印流程办理。\n\n");
            return;
        }
        int index = 1;
        for (OpinionView item : actionable) {
            sb.append(index++).append(". ");
            if (StrUtil.isNotBlank(item.getSuggestion())) {
                sb.append(item.getSuggestion());
            } else {
                sb.append("针对「").append(StrUtil.blankToDefault(item.getTitle(), "风险项"))
                        .append("」完成条款修订或取得对方书面确认。");
            }
            if (StrUtil.isNotBlank(item.getParagraphId())) {
                sb.append("（位置：").append(item.getParagraphId()).append("）");
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private static String extractThematicHint(List<OpinionView> opinions, String... keywords) {
        List<String> parts = new ArrayList<>();
        for (OpinionView item : opinions) {
            if (!matchesAnyTheme(item, keywords)) {
                continue;
            }
            String text = StrUtil.blankToDefault(item.getContent(), "");
            if (StrUtil.isNotBlank(item.getSuggestion())) {
                text = text + " " + item.getSuggestion();
            }
            parts.add(text.trim());
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(" ", parts) + "\n\n";
    }

    private static boolean matchesAnyTheme(OpinionView item, String... keywords) {
        String haystack = (StrUtil.blankToDefault(item.getClauseType(), "") + " "
                + StrUtil.blankToDefault(item.getTitle(), "") + " "
                + StrUtil.blankToDefault(item.getContent(), "") + " "
                + StrUtil.blankToDefault(item.getSuggestion(), "")).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, List<OpinionView>> groupByClauseType(List<OpinionView> opinions) {
        Map<String, List<OpinionView>> map = new LinkedHashMap<>();
        for (OpinionView item : opinions) {
            String key = StrUtil.blankToDefault(item.getClauseType(), "其他条款");
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private static String maxRiskLevel(List<OpinionView> group) {
        LegalRiskLevelEnum max = LegalRiskLevelEnum.LOW;
        for (OpinionView item : group) {
            LegalRiskLevelEnum current = normalizeLevel(item.getRiskLevel());
            if (riskOrder(current) > riskOrder(max)) {
                max = current;
            }
        }
        return max.getCode();
    }

    private static String complianceStatus(String riskLevel) {
        return LegalComplianceResultEnum.fromRiskLevel(normalizeLevel(riskLevel)).getLabel();
    }

    private static String summarizeSuggestions(List<OpinionView> group) {
        return group.stream()
                .map(OpinionView::getSuggestion)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    private static List<OpinionView> sortByRisk(List<OpinionView> opinions) {
        return opinions.stream()
                .sorted((a, b) -> Integer.compare(
                        riskOrder(normalizeLevel(b.getRiskLevel())),
                        riskOrder(normalizeLevel(a.getRiskLevel()))))
                .collect(Collectors.toList());
    }

    private static int riskOrder(LegalRiskLevelEnum riskLevel) {
        return switch (riskLevel) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            default -> 1;
        };
    }

    private static long countByLevel(List<OpinionView> opinions, LegalRiskLevelEnum level) {
        return opinions.stream()
                .filter(o -> level == normalizeLevel(o.getRiskLevel()))
                .count();
    }

    private static LegalRiskLevelEnum normalizeLevel(String riskLevel) {
        return LegalRiskLevelEnum.normalize(riskLevel);
    }

    private static String toChineseLevel(String riskLevel) {
        return normalizeLevel(riskLevel).getLabel();
    }

    private static String formatRemark(OpinionView item) {
        if (StrUtil.isNotBlank(item.getParagraphId())) {
            return "段落 " + item.getParagraphId();
        }
        if (StrUtil.isNotBlank(item.getClauseType())) {
            return item.getClauseType();
        }
        return "—";
    }

    private static String formatClauseRef(OpinionView item) {
        if (StrUtil.isNotBlank(item.getReferenceClause())) {
            return item.getReferenceClause();
        }
        if (StrUtil.isNotBlank(item.getParagraphId())) {
            return item.getParagraphId();
        }
        return StrUtil.blankToDefault(item.getClauseType(), "—");
    }

    private static String escapeCell(String text) {
        if (text == null) {
            return "—";
        }
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }

    @Data
    public static class OpinionView {
        private String clauseType;
        private String riskLevel;
        private String title;
        private String content;
        private String suggestion;
        private String paragraphId;
        private String referenceClause;
    }

}
