package com.laby.module.ai.framework.knowledge.retrieval.bo;

import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Universal RAG 统一检索请求
 */
@Data
@Accessors(chain = true)
public class AiKnowledgeRetrievalRequest {

    private Long knowledgeId;

    private String query;

    private Integer topK;

    private Double similarityThreshold;

    /** 可空，自动分类 */
    private AiQueryIntentEnum intent;

    private boolean enableMultiQuery = true;

    private boolean enableHybrid = true;

    /** 输出：召回诊断 */
    private RecallDiagnostics diagnostics;

}
