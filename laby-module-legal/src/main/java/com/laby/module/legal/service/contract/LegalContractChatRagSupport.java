package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.service.auditrule.LegalAuditContextService;
import com.laby.module.legal.service.auditrule.bo.LegalAuditContextResult;
import com.laby.module.legal.service.retrieval.LegalRetrievalLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 合同问答 RAG：知识库召回与检索日志。
 */
@Component
public class LegalContractChatRagSupport {

    private static final int QA_RAG_TOP_K = 5;

    @Resource
    private LegalAuditContextService auditContextService;
    @Resource
    private LegalRetrievalLogService retrievalLogService;

    public void appendKnowledgeContext(List<AiMessage> messages, LegalContractDO contract, String userMessage) {
        LegalAuditContextResult knowledgeContext = auditContextService.buildChatKnowledgeContext(
                contract, userMessage.trim());
        if (StrUtil.isNotBlank(knowledgeContext.getSupplementMarkdown())) {
            messages.add(new AiMessage().setRole(AiMessageRoleEnum.SYSTEM)
                    .setContent("【知识库参考】" + knowledgeContext.getSupplementMarkdown()));
        }
        int auditRound = contract.getAuditRound() == null ? 1 : contract.getAuditRound();
        retrievalLogService.logKnowledgeRetrieval(LegalRetrievalLogService.BIZ_TYPE_QA,
                contract.getId(), auditRound, null, userMessage.trim(), QA_RAG_TOP_K, knowledgeContext);
    }

}
