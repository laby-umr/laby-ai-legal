package com.laby.module.legal.service.ai.kernel.bo;

import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 正式审核内核命令
 */
@Data
@Builder
public class LegalAuditKernelCommand {

    private LegalContractDO contract;

    private List<LegalContractParagraphDO> paragraphs;

    private int auditRound;

    private boolean failFast;

    private boolean playbookEnabled;

}
