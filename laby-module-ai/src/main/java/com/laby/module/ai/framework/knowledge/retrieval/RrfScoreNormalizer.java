package com.laby.module.ai.framework.knowledge.retrieval;

/**
 * RRF 分值归一化：RRF 原始分约在 0.01～0.05，不可与 cosine/rerank 阈值直接比较。
 */
public final class RrfScoreNormalizer {

    private RrfScoreNormalizer() {
    }

    /**
     * 将候选集内 RRF 分映射到 [0.50, 1.00]，供 minAnswerScore 等守卫在 Rerank 降级时使用。
     */
    public static double normalizeToDisplayScore(double rrfScore, double maxRrfScore) {
        if (maxRrfScore <= 0) {
            return 0.75;
        }
        return 0.5 + 0.5 * (rrfScore / maxRrfScore);
    }

}
