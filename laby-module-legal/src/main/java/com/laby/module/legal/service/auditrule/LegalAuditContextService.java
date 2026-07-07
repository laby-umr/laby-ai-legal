package com.laby.module.legal.service.auditrule;

import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.service.auditrule.bo.LegalAuditContextResult;

import java.util.List;

/**
 * 为 AI 审核组装全局规则与知识库 RAG 上下文。
 */
public interface LegalAuditContextService {

    /**
     * 组装 AI 审核补充上下文（全局规则与知识库 RAG）
     */
    String buildAuditSupplement(LegalContractDO contract, List<LegalContractParagraphDO> batchParagraphs);

    /**
     * 结构化审核上下文（含规则/RAG 引用，供证据落库与检索日志）。
     */
    LegalAuditContextResult buildAuditContext(LegalContractDO contract, List<LegalContractParagraphDO> batchParagraphs);

    /**
     * 合同问答：按用户问题检索知识库片段。
     */
    LegalAuditContextResult buildChatKnowledgeContext(LegalContractDO contract, String userMessage);

    /**
     * 记录本批次 RAG 检索日志。
     */
    void logRetrieval(Long contractId, int auditRound, int batchIndex, LegalAuditContextResult context);

}
