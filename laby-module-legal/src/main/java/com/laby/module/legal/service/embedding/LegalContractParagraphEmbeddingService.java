package com.laby.module.legal.service.embedding;

import com.laby.module.legal.service.embedding.bo.LegalParagraphVectorHitBO;

import java.util.List;

/**
 * 法务合同段落向量索引 Service
 */
public interface LegalContractParagraphEmbeddingService {

    /**
     * 解析成功后异步向量化合同段落
     */
    void embedContractAsync(Long contractId);

    /**
     * 按语义相似度检索段落（限定 contract_id）
     *
     * @return 命中列表，按相似度降序；向量不可用或无结果时返回空列表
     */
    List<LegalParagraphVectorHitBO> searchByVector(Long contractId, String query, int limit);

    /**
     * 删除合同下全部段落向量（重解析前调用）
     */
    void deleteByContractId(Long contractId);

    /**
     * 合同是否已有成功的向量索引
     */
    boolean hasEmbeddings(Long contractId);

}
