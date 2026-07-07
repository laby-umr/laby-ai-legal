package com.laby.module.legal.service.orchestrator.bo;

import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LegalAiAuditPipelineCommand {

    private AiLlmClient llmClient;
    private String systemPrompt;
    private LegalContractDO contract;
    private List<LegalContractParagraphDO> paragraphs;
    private int auditRound;
    private boolean failFast;
    /** 审核输出 maxTokens，覆盖模型默认配置 */
    private Integer maxTokens;

}
