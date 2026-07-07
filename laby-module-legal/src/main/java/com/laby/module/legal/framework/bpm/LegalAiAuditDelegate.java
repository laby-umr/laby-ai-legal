package com.laby.module.legal.framework.bpm;

import com.laby.module.legal.service.contract.LegalAiAuditService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("legalAiAuditDelegate")
public class LegalAiAuditDelegate implements JavaDelegate {

    @Resource
    private LegalAiAuditService aiAuditService;

    @Override
    public void execute(DelegateExecution execution) {
        Long contractId = LegalContractParseDelegate.getContractId(execution);
        int round = resolveRound(execution);
        log.info("[LegalAiAuditDelegate] contractId={} round={} enqueue only", contractId, round);
        aiAuditService.enqueueAuditForBpm(contractId, round);
    }

    private int resolveRound(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        if (activityId != null && activityId.contains("Round2")) {
            return 2;
        }
        Object roundVar = execution.getVariable("auditRound");
        if (roundVar instanceof Integer i) {
            return i;
        }
        return 1;
    }

}
