package com.laby.module.ai.framework.knowledge.retrieval;

import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallDiagnosticsConverterTest {

    @Test
    void toMap_shouldSerializeRecallDiagnostics() {
        RecallDiagnostics diagnostics = new RecallDiagnostics()
                .setIntent(AiQueryIntentEnum.GENERAL)
                .setQueryVariants(List.of("q1", "q2"))
                .setDenseHitCount(3)
                .setSparseHitCount(2)
                .setFusedHitCount(4)
                .setRerankHitCount(2)
                .setTopScore(0.88)
                .setLatencyMs(120L)
                .setNotes(List.of("sparse degraded"));

        Map<String, Object> map = RecallDiagnosticsConverter.toMap(diagnostics);

        assertEquals("general", map.get("intent"));
        assertEquals(List.of("q1", "q2"), map.get("queryVariants"));
        assertEquals(3, map.get("denseHitCount"));
        assertEquals(2, map.get("sparseHitCount"));
        assertEquals(4, map.get("fusedHitCount"));
        assertEquals(2, map.get("rerankHitCount"));
        assertEquals(0.88, map.get("topScore"));
        assertEquals(120L, map.get("latencyMs"));
        assertEquals(List.of("sparse degraded"), map.get("notes"));
    }

    @Test
    void mergeChatDiagnostics_shouldMergeItemsAndTotalHitCount() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("knowledgeId", 1L);
        item.put("denseHitCount", 2);

        Map<String, Object> merged = RecallDiagnosticsConverter.mergeChatDiagnostics(List.of(item), 5);

        assertEquals(5, merged.get("totalHitCount"));
        assertEquals(List.of(item), merged.get("items"));
    }

    @Test
    void withNoAnswerGuard_shouldMarkGuardTriggered() {
        Map<String, Object> merged = RecallDiagnosticsConverter.withNoAnswerGuard(new LinkedHashMap<>(), true);

        assertTrue((Boolean) merged.get("noAnswerGuard"));
    }

}
