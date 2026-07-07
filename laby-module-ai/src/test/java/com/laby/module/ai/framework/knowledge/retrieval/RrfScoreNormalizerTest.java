package com.laby.module.ai.framework.knowledge.retrieval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RrfScoreNormalizerTest {

    @Test
    void normalize_mapsRrfToDisplayRange() {
        double max = 1.0 / 61.0;
        double top = RrfScoreNormalizer.normalizeToDisplayScore(max, max);
        double second = RrfScoreNormalizer.normalizeToDisplayScore(max / 2, max);

        assertEquals(1.0, top, 1e-9);
        assertTrue(second > 0.5);
        assertTrue(second < top);
    }

    @Test
    void normalize_handlesZeroMax() {
        assertEquals(0.75, RrfScoreNormalizer.normalizeToDisplayScore(0.01, 0), 1e-9);
    }

}
