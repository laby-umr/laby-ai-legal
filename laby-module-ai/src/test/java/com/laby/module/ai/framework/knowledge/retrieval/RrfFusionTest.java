package com.laby.module.ai.framework.knowledge.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RrfFusionTest {

    @Test
    void fuse_mergesDenseAndSparseWithDedup() {
        List<Long> dense = List.of(1L, 2L, 3L);
        List<Long> sparse = List.of(2L, 4L, 5L);

        Map<Long, Double> fused = RrfFusion.fuse(60, 1.0, dense, 1.2, sparse);

        assertEquals(5, fused.size());
        assertTrue(fused.containsKey(1L));
        assertTrue(fused.containsKey(2L));
        assertTrue(fused.containsKey(4L));
        assertTrue(fused.get(2L) > fused.get(1L));
    }

    @Test
    void fuse_keepsHighestContributionForDuplicateSegment() {
        List<RrfFusion.WeightedRankedList> lists = List.of(
                new RrfFusion.WeightedRankedList(1.0, List.of(10L, 20L)),
                new RrfFusion.WeightedRankedList(1.0, List.of(20L, 30L))
        );

        Map<Long, Double> fused = RrfFusion.fuse(60, lists);

        assertEquals(1.0 / 61.0, fused.get(10L), 1e-9);
        assertEquals(1.0 / 61.0, fused.get(20L), 1e-9);
        assertEquals(1.0 / 62.0, fused.get(30L), 1e-9);
    }

    @Test
    void fuse_sortsByScoreDescending() {
        Map<Long, Double> fused = RrfFusion.fuse(60, 1.0, List.of(100L), 1.0, List.of(200L, 300L));

        List<Long> ordered = fused.keySet().stream().toList();
        assertTrue(fused.get(ordered.get(0)) >= fused.get(ordered.get(1)));
        assertEquals(3, ordered.size());
    }

}
