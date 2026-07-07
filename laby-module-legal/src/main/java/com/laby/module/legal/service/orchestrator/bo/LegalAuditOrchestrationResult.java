package com.laby.module.legal.service.orchestrator.bo;

import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 审核编排结果
 */
@Data
@Builder
public class LegalAuditOrchestrationResult {

    private String traceId;

    @Builder.Default
    private List<LegalAuditOpinionDraftBO> deterministicOpinions = new ArrayList<>();

    private int deterministicCount;

}
