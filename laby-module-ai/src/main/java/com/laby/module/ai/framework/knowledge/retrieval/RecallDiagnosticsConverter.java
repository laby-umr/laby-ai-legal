package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 召回诊断与持久化 / API 之间的转换
 */
public final class RecallDiagnosticsConverter {

    private RecallDiagnosticsConverter() {
    }

    public static Map<String, Object> toMap(RecallDiagnostics diagnostics) {
        if (diagnostics == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        if (diagnostics.getIntent() != null) {
            map.put("intent", diagnostics.getIntent().getCode());
        }
        if (CollUtil.isNotEmpty(diagnostics.getQueryVariants())) {
            map.put("queryVariants", new ArrayList<>(diagnostics.getQueryVariants()));
        }
        map.put("denseHitCount", diagnostics.getDenseHitCount());
        map.put("sparseHitCount", diagnostics.getSparseHitCount());
        map.put("fusedHitCount", diagnostics.getFusedHitCount());
        map.put("rerankHitCount", diagnostics.getRerankHitCount());
        if (diagnostics.getTopScore() != null) {
            map.put("topScore", diagnostics.getTopScore());
        }
        map.put("latencyMs", diagnostics.getLatencyMs());
        if (CollUtil.isNotEmpty(diagnostics.getPaths())) {
            map.put("paths", new LinkedHashMap<>(diagnostics.getPaths()));
        }
        if (CollUtil.isNotEmpty(diagnostics.getNotes())) {
            map.put("notes", new ArrayList<>(diagnostics.getNotes()));
        }
        return map;
    }

    public static Map<String, Object> mergeChatDiagnostics(List<Map<String, Object>> items, int totalHitCount) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("totalHitCount", totalHitCount);
        if (CollUtil.isNotEmpty(items)) {
            merged.put("items", items);
        }
        return merged;
    }

    public static Map<String, Object> withNoAnswerGuard(Map<String, Object> diagnostics, boolean triggered) {
        if (diagnostics == null) {
            diagnostics = new LinkedHashMap<>();
        }
        diagnostics.put("noAnswerGuard", triggered);
        return diagnostics;
    }

}