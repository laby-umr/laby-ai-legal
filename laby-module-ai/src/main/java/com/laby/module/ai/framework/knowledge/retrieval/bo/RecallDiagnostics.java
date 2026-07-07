package com.laby.module.ai.framework.knowledge.retrieval.bo;

import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 召回诊断信息
 */
@Data
@Accessors(chain = true)
public class RecallDiagnostics {

    private AiQueryIntentEnum intent;

    private List<String> queryVariants = new ArrayList<>();

    private int denseHitCount;

    private int sparseHitCount;

    private int fusedHitCount;

    private int rerankHitCount;

    private Double topScore;

    private long latencyMs;

    private Map<String, Object> paths = new LinkedHashMap<>();

    private List<String> notes = new ArrayList<>();

}
