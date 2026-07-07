package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.mysql.knowledge.AiKnowledgeSegmentMapper;
import com.laby.module.ai.dal.mysql.knowledge.dto.AiKnowledgeSparseSearchRow;
import com.laby.module.ai.enums.ErrorCodeConstants;
import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQL FULLTEXT 稀疏检索
 */
@Slf4j
@RequiredArgsConstructor
public class SparseRetrievalEngine {

    private final AiKnowledgeSegmentMapper segmentMapper;
    private final KnowledgeRetrievalProperties properties;

    public List<Long> search(Long knowledgeId, String query, int limit) {
        return search(knowledgeId, query, limit, null);
    }

    public List<Long> search(Long knowledgeId, String query, int limit, RecallDiagnostics diagnostics) {
        if (!properties.getHybrid().isEnabled() || !properties.getHybrid().isSparseEnabled()) {
            return List.of();
        }
        if (knowledgeId == null || StrUtil.isBlank(query) || limit <= 0) {
            return List.of();
        }
        String normalizedQuery = SparseTextNormalizer.normalize(query);
        if (StrUtil.isBlank(normalizedQuery)) {
            return List.of();
        }
        List<AiKnowledgeSparseSearchRow> rows = searchSparseRows(knowledgeId, normalizedQuery, limit, diagnostics);
        if (CollUtil.isEmpty(rows)) {
            return List.of();
        }
        List<Long> segmentIds = new ArrayList<>(rows.size());
        for (AiKnowledgeSparseSearchRow row : rows) {
            if (row.getId() != null) {
                segmentIds.add(row.getId());
            }
        }
        return segmentIds;
    }

    private List<AiKnowledgeSparseSearchRow> searchSparseRows(Long knowledgeId, String normalizedQuery,
                                                                int limit, RecallDiagnostics diagnostics) {
        try {
            return segmentMapper.selectListBySparseSearch(knowledgeId, normalizedQuery, limit);
        } catch (Exception combinedEx) {
            log.debug("[SparseRetrievalEngine][组合 FULLTEXT 不可用，尝试 sparse_text 单列 knowledgeId={}]",
                    knowledgeId, combinedEx);
            try {
                return segmentMapper.selectListBySparseTextOnlySearch(knowledgeId, normalizedQuery, limit);
            } catch (Exception sparseOnlyEx) {
                log.warn("[SparseRetrievalEngine][sparse 检索失败，降级 Dense knowledgeId={} query={}]",
                        knowledgeId, normalizedQuery, sparseOnlyEx);
                if (diagnostics != null) {
                    diagnostics.getNotes().add(ErrorCodeConstants.KNOWLEDGE_RETRIEVAL_SPARSE_FAIL.getMsg());
                }
                return List.of();
            }
        }
    }

}
