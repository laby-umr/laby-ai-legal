package com.laby.module.legal.service.ai.kernel.bo;

import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 审核内核执行结果
 */
@Data
@Builder
public class LegalAuditKernelResult {

    private List<LegalAiAuditOpinionItemBO> items;

    private int deterministicCount;

    private int paragraphCount;

    /** 实际使用的系统提示词（便于单测比对） */
    private String systemPrompt;

    private Long modelId;

}
