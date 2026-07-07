package com.laby.module.legal.framework.bpm;

import com.laby.module.legal.service.bpm.LegalContractBpmAuditSignalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * ReceiveTask 进入时：若 AI 审核已终态则立即 trigger（处理 Consumer 先于 BPM 到达的竞态）。
 */
@Slf4j
@Component("legalAiAuditReceiveListener")
public class LegalAiAuditReceiveListener implements ExecutionListener {

    @Resource
    private LegalContractBpmAuditSignalService bpmAuditSignalService;

    @Override
    public void notify(DelegateExecution execution) {
        Long contractId = LegalContractParseDelegate.getContractId(execution);
        log.info("[LegalAiAuditReceiveListener][contractId={} activity={}]",
                contractId, execution.getCurrentActivityId());
        bpmAuditSignalService.signalAwaitAiRound2IfSettled(contractId);
    }

}
