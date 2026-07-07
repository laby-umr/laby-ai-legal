package com.laby.module.ai.dal.mysql.knowledge.dto;

import lombok.Data;

/**
 * MySQL FULLTEXT 稀疏检索结果行
 */
@Data
public class AiKnowledgeSparseSearchRow {

    private Long id;

    private Double sparseScore;

}
