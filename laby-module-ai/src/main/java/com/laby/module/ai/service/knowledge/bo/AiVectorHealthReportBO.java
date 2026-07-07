package com.laby.module.ai.service.knowledge.bo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AiVectorHealthReportBO {

    @Builder.Default
    private int knowledgeCount = 0;
    @Builder.Default
    private int segmentScanned = 0;
    /** 启用状态但 vector_id 为空 */
    @Builder.Default
    private int missingVectorId = 0;
    /** DB 有 vector_id 但 Qdrant 无对应 point */
    @Builder.Default
    private int missingInQdrant = 0;
    /** payload 中 embeddingModelId 与知识库当前配置不一致 */
    @Builder.Default
    private int modelMismatch = 0;
    /** 启用 Sparse 检索但 sparse_text 为空（历史入库未回填） */
    @Builder.Default
    private int missingSparseText = 0;
    @Builder.Default
    private int repaired = 0;
    @Builder.Default
    private int repairFailed = 0;
    @Builder.Default
    private int sparseTextRepaired = 0;
    @Builder.Default
    private boolean dryRun = false;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

}
