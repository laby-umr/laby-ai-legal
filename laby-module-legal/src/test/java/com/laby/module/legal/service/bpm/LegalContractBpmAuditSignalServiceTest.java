package com.laby.module.legal.service.bpm;

import com.laby.module.bpm.api.task.BpmProcessTaskApi;
import com.laby.module.legal.controller.admin.contract.vo.LegalAiAuditProgressRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.enums.contract.LegalContractBpmAuditConstants;
import com.laby.module.legal.service.contract.LegalAiAuditProgressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalContractBpmAuditSignalServiceTest {

    @InjectMocks
    private LegalContractBpmAuditSignalService signalService;

    @Mock
    private LegalContractMapper contractMapper;
    @Mock
    private LegalAiAuditProgressService auditProgressService;
    @Mock
    private BpmProcessTaskApi bpmProcessTaskApi;

    @Test
    void signalAwaitAiRound2IfSettled_completed_triggersReceiveTask() {
        when(contractMapper.selectById(1L)).thenReturn(LegalContractDO.builder()
                .id(1L).processInstanceId("pi-1").build());
        LegalAiAuditProgressRespVO progress = new LegalAiAuditProgressRespVO();
        progress.setStatus("COMPLETED");
        when(auditProgressService.get(1L)).thenReturn(progress);

        signalService.signalAwaitAiRound2IfSettled(1L);

        verify(bpmProcessTaskApi).triggerTask("pi-1", LegalContractBpmAuditConstants.RECEIVE_AWAIT_AI_ROUND2);
    }

    @Test
    void signalAwaitAiRound2IfSettled_running_skipsTrigger() {
        when(contractMapper.selectById(1L)).thenReturn(LegalContractDO.builder()
                .id(1L).processInstanceId("pi-1").build());
        LegalAiAuditProgressRespVO progress = new LegalAiAuditProgressRespVO();
        progress.setStatus("RUNNING");
        when(auditProgressService.get(1L)).thenReturn(progress);

        signalService.signalAwaitAiRound2IfSettled(1L);

        verify(bpmProcessTaskApi, never()).triggerTask(eq("pi-1"), eq(LegalContractBpmAuditConstants.RECEIVE_AWAIT_AI_ROUND2));
    }

    @Test
    void signalAwaitAiRound2IfSettled_noProcessInstance_skips() {
        when(contractMapper.selectById(1L)).thenReturn(LegalContractDO.builder().id(1L).build());

        signalService.signalAwaitAiRound2IfSettled(1L);

        verify(bpmProcessTaskApi, never()).triggerTask(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

}
