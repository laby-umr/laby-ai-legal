package com.laby.module.legal.service.eval;

import com.laby.module.legal.enums.LegalEvalConstants;
import com.laby.module.legal.service.eval.bo.LegalAuditEvalReportBO;

/**
 * Playbook 黄金集 CI 门禁
 */
public final class LegalPlaybookEvalGate {

    private LegalPlaybookEvalGate() {
    }

    /**
     * 解析 CI 最低通过率（默认 100%）
     */
    public static double resolveMinPassRate() {
        String raw = System.getProperty(LegalEvalConstants.MIN_PASS_RATE_PROPERTY);
        if (raw == null || raw.isBlank()) {
            return LegalEvalConstants.DEFAULT_MIN_PASS_RATE;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("非法 " + LegalEvalConstants.MIN_PASS_RATE_PROPERTY + ": " + raw, ex);
        }
    }

    /**
     * 断言评测报告达到 CI 门禁
     */
    public static void assertGate(LegalAuditEvalReportBO report) {
        double minPassRate = resolveMinPassRate();
        if (report.meetsThreshold(minPassRate)) {
            return;
        }
        throw new IllegalStateException(String.format(
                "Playbook 黄金集未达门禁：passRate=%.2f%% < minPassRate=%.2f%%，失败用例=%s",
                report.passRate() * 100D,
                minPassRate * 100D,
                report.getFailedCaseIds()));
    }

}
