package com.laby.module.ai.framework.knowledge.retrieval;

import com.laby.framework.common.exception.ErrorCode;
import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 召回诊断收集与慢查询日志
 */
@Slf4j
public class RecallDiagnosticsCollector {

    private final KnowledgeRetrievalProperties properties;

    public RecallDiagnosticsCollector(KnowledgeRetrievalProperties properties) {
        this.properties = properties;
    }

    public RecallDiagnostics start() {
        RecallDiagnostics diagnostics = new RecallDiagnostics();
        diagnostics.getPaths().put("startedAtMs", System.currentTimeMillis());
        return diagnostics;
    }

    public void recordIntent(RecallDiagnostics diagnostics, AiQueryIntentEnum intent) {
        if (diagnostics == null) {
            return;
        }
        diagnostics.setIntent(intent);
    }

    public void recordVariants(RecallDiagnostics diagnostics, List<String> variants) {
        if (diagnostics == null) {
            return;
        }
        diagnostics.setQueryVariants(variants);
    }

    public void recordDenseHits(RecallDiagnostics diagnostics, int count) {
        if (diagnostics == null) {
            return;
        }
        diagnostics.setDenseHitCount(count);
    }

    public void recordSparseHits(RecallDiagnostics diagnostics, int count) {
        if (diagnostics == null) {
            return;
        }
        diagnostics.setSparseHitCount(count);
    }

    public void recordFusedHits(RecallDiagnostics diagnostics, int count) {
        if (diagnostics == null) {
            return;
        }
        diagnostics.setFusedHitCount(count);
    }

    public void recordRerankHits(RecallDiagnostics diagnostics, int count, Double topScore) {
        if (diagnostics == null) {
            return;
        }
        diagnostics.setRerankHitCount(count);
        diagnostics.setTopScore(topScore);
    }

    public void recordDegrade(RecallDiagnostics diagnostics, ErrorCode errorCode) {
        if (diagnostics == null || errorCode == null) {
            return;
        }
        diagnostics.getNotes().add(errorCode.getMsg());
        diagnostics.getPaths().put("degradeCode_" + errorCode.getCode(), errorCode.getCode());
    }

    public void finish(Long knowledgeId, String query, RecallDiagnostics diagnostics, long startedAtMs) {
        if (diagnostics == null) {
            return;
        }
        long latencyMs = Math.max(0, System.currentTimeMillis() - startedAtMs);
        diagnostics.setLatencyMs(latencyMs);
        if (!properties.getDiagnostics().isEnabled()) {
            return;
        }
        if (latencyMs >= properties.getDiagnostics().getLogSlowMs()) {
            log.info("[KnowledgeRetrieval][slow] knowledgeId={} query={} intent={} dense={} sparse={} fused={} rerank={} topScore={} latencyMs={}",
                    knowledgeId, query, diagnostics.getIntent(), diagnostics.getDenseHitCount(),
                    diagnostics.getSparseHitCount(), diagnostics.getFusedHitCount(),
                    diagnostics.getRerankHitCount(), diagnostics.getTopScore(), latencyMs);
        } else {
            log.debug("[KnowledgeRetrieval] knowledgeId={} query={} intent={} dense={} sparse={} fused={} rerank={} topScore={} latencyMs={}",
                    knowledgeId, query, diagnostics.getIntent(), diagnostics.getDenseHitCount(),
                    diagnostics.getSparseHitCount(), diagnostics.getFusedHitCount(),
                    diagnostics.getRerankHitCount(), diagnostics.getTopScore(), latencyMs);
        }
    }

}
