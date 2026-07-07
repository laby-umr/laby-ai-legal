package com.laby.module.legal.service.bpm.listener;

import com.laby.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.laby.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.laby.module.legal.enums.LegalContractConstants;
import com.laby.module.legal.service.contract.LegalContractService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class LegalContractStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private LegalContractService contractService;

    @Override
    protected String getProcessDefinitionKey() {
        return LegalContractConstants.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        contractService.updateBpmStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}
