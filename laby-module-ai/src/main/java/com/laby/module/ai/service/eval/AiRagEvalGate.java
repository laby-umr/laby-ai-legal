package com.laby.module.ai.service.eval;

import com.laby.module.ai.enums.AiRagEvalConstants;
import com.laby.module.ai.service.eval.bo.AiRagEvalReportBO;

/**
 * RAG 黄金集 CI 门禁
 */
public final class AiRagEvalGate {

    private AiRagEvalGate() {
    }

    public static double resolveMinPassRate() {
        return resolveDoubleProperty(AiRagEvalConstants.MIN_PASS_RATE_PROPERTY,
                AiRagEvalConstants.DEFAULT_MIN_PASS_RATE);
    }

    public static double resolveMinHitAtKRate() {
        return resolveDoubleProperty(AiRagEvalConstants.MIN_HIT_AT_K_RATE_PROPERTY,
                AiRagEvalConstants.DEFAULT_MIN_HIT_AT_K_RATE);
    }

    public static void assertGate(AiRagEvalReportBO report) {
        double minPassRate = resolveMinPassRate();
        double minHitAtKRate = resolveMinHitAtKRate();
        if (!report.meetsPassRateThreshold(minPassRate)) {
            throw new IllegalStateException(String.format(
                    "RAG 黄金集未达通过率门禁：passRate=%.2f%% < minPassRate=%.2f%%，失败用例=%s",
                    report.passRate() * 100D, minPassRate * 100D, report.getFailedCaseIds()));
        }
        if (!report.meetsHitAtKThreshold(minHitAtKRate)) {
            throw new IllegalStateException(String.format(
                    "RAG 黄金集未达 Hit@K 门禁：hitAtKRate=%.2f%% < minHitAtKRate=%.2f%%",
                    report.getHitAtKRate() * 100D, minHitAtKRate * 100D));
        }
    }

    private static double resolveDoubleProperty(String propertyName, double defaultValue) {
        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("非法 " + propertyName + ": " + raw, ex);
        }
    }

}
