package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion（RRF，k=60 默认）
 */
public final class RrfFusion {

    private RrfFusion() {
    }

    public record WeightedRankedList(double weight, List<Long> segmentIds) {
    }

    /**
     * 融合多路排序列表，按 segmentId 去重并保留最高 RRF 分。
     *
     * @param k       RRF 常数
     * @param lists   加权排序列表
     * @return segmentId → RRF 分数（降序）
     */
    public static Map<Long, Double> fuse(int k, List<WeightedRankedList> lists) {
        if (CollUtil.isEmpty(lists)) {
            return Map.of();
        }
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (WeightedRankedList list : lists) {
            if (list == null || CollUtil.isEmpty(list.segmentIds())) {
                continue;
            }
            double weight = list.weight() <= 0 ? 1.0 : list.weight();
            for (int rank = 0; rank < list.segmentIds().size(); rank++) {
                Long segmentId = list.segmentIds().get(rank);
                if (segmentId == null) {
                    continue;
                }
                double contribution = weight / (k + rank + 1);
                scores.merge(segmentId, contribution, Math::max);
            }
        }
        return sortByScoreDesc(scores);
    }

    public static Map<Long, Double> fuse(int k, double denseWeight, List<Long> denseIds,
                                         double sparseWeight, List<Long> sparseIds) {
        List<WeightedRankedList> lists = new ArrayList<>(2);
        if (CollUtil.isNotEmpty(denseIds)) {
            lists.add(new WeightedRankedList(denseWeight, denseIds));
        }
        if (CollUtil.isNotEmpty(sparseIds)) {
            lists.add(new WeightedRankedList(sparseWeight, sparseIds));
        }
        return fuse(k, lists);
    }

    private static Map<Long, Double> sortByScoreDesc(Map<Long, Double> scores) {
        List<Map.Entry<Long, Double>> entries = new ArrayList<>(scores.entrySet());
        entries.sort(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey));
        Map<Long, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

}
