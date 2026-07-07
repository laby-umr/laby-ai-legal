package com.laby.module.legal.framework.bpm;

import com.laby.module.legal.service.contract.LegalContractParseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("legalContractParseDelegate")
public class LegalContractParseDelegate implements JavaDelegate {

    @Resource
    private LegalContractParseService parseService;

    @Override
    public void execute(DelegateExecution execution) {
        Long contractId = getContractId(execution);
        log.info("[LegalContractParseDelegate] contractId={}", contractId);
        parseService.parseForBpm(contractId);
    }

    static Long getContractId(DelegateExecution execution) {
        Object value = execution.getVariable("contractId");
        if (value instanceof Long l) {
            return l;
        }
        return Long.valueOf(String.valueOf(value));
    }

}
