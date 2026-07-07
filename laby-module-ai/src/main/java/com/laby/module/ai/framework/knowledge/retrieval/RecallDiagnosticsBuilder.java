package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeRecallDiagnosticsVO;
import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 召回诊断构建（检索测试页 / 管理端）
 */
public final class RecallDiagnosticsBuilder {

    private RecallDiagnosticsBuilder() {
    }

    public static RecallDiagnostics start(String query, KnowledgeRetrievalProperties properties) {
        RecallDiagnostics diagnostics = new RecallDiagnostics();
        AiQueryIntentEnum intent = QueryIntentClassifier.classify(query);
        diagnostics.setIntent(intent);
        diagnostics.setQueryVariants(buildQueryVariants(query, properties));
        return diagnostics;
    }

    public static void finish(RecallDiagnostics diagnostics, List<AiKnowledgeSegmentSearchRespBO> segments,
                              long startedAtNanos) {
        int hitCount = CollUtil.size(segments);
        diagnostics.setDenseHitCount(hitCount);
        diagnostics.setSparseHitCount(0);
        diagnostics.setFusedHitCount(hitCount);
        diagnostics.setRerankHitCount(hitCount);
        diagnostics.setTopScore(CollUtil.isEmpty(segments) ? null : segments.get(0).getScore());
        diagnostics.setLatencyMs((System.nanoTime() - startedAtNanos) / 1_000_000);
        if (hitCount == 0) {
            diagnostics.getNotes().add("无段落通过相似度阈值");
        }
    }

    public static AiKnowledgeRecallDiagnosticsVO toVo(RecallDiagnostics diagnostics) {
        if (diagnostics == null) {
            return null;
        }
        AiKnowledgeRecallDiagnosticsVO vo = BeanUtils.toBean(diagnostics, AiKnowledgeRecallDiagnosticsVO.class);
        if (diagnostics.getIntent() != null) {
            vo.setIntent(diagnostics.getIntent().getCode());
        }
        return vo;
    }

    private static List<String> buildQueryVariants(String query, KnowledgeRetrievalProperties properties) {
        List<String> variants = new ArrayList<>();
        if (StrUtil.isNotBlank(query)) {
            variants.add(StrUtil.trim(query));
        }
        if (!properties.getMultiQuery().isEnabled() || variants.isEmpty()) {
            return variants;
        }
        String compact = StrUtil.removeAll(query, ' ');
        if (StrUtil.isNotBlank(compact) && !variants.contains(compact)) {
            variants.add(compact);
        }
        int max = Math.max(1, properties.getMultiQuery().getMaxVariants());
        return variants.size() <= max ? variants : variants.subList(0, max);
    }

    public static RecallDiagnostics emptyPaths(RecallDiagnostics diagnostics) {
        if (diagnostics.getPaths().isEmpty()) {
            diagnostics.setPaths(new LinkedHashMap<>());
        }
        return diagnostics;
    }

}
