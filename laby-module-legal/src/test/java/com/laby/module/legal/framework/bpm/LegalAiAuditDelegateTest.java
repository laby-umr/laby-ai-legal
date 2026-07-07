package com.laby.module.legal.framework.bpm;

import com.laby.module.legal.service.contract.LegalAiAuditService;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalAiAuditDelegateTest {

    @InjectMocks
    private LegalAiAuditDelegate delegate;

    @Mock
    private LegalAiAuditService aiAuditService;
    @Mock
    private DelegateExecution execution;

    @Test
    void execute_enqueuesOnly_doesNotBlock() {
        when(execution.getVariable("contractId")).thenReturn(10L);
        when(execution.getCurrentActivityId()).thenReturn("aiRound2Enqueue");

        delegate.execute(execution);

        verify(aiAuditService).enqueueAuditForBpm(10L, 2);
    }

}
