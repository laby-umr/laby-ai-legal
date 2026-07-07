package com.laby.module.legal.service.contract.bo;

import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * AI 审核意见与报告持久化命令
 */
@Data
@Builder
public class LegalContractAuditPersistCommand {

    private Long contractId;

    private LegalContractDO contract;

    private int auditRound;

    private List<LegalAiAuditOpinionItemBO> items;
}
