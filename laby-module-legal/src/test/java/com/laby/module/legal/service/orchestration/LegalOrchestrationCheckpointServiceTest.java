package com.laby.module.legal.service.orchestration;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalOrchestrationCheckpointServiceTest {

    @Mock
    private LegalOrchestrationSessionMapper sessionMapper;

    @InjectMocks
    private LegalOrchestrationCheckpointService checkpointService;

    @Test
    void savePhaseCheckpoint_shouldPersistJson() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(1L)
                .conversationId(99L)
                .phase("INIT")
                .modelId(3L)
                .build();

        checkpointService.savePhaseCheckpoint(session, "CLASSIFY_PENDING");

        verify(sessionMapper).updateById(any(LegalOrchestrationSessionDO.class));
    }

    @Test
    void loadCheckpoint_shouldParseStoredJson() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(1L)
                .checkpointJson("""
                        {"phase":"CREATE_PENDING","conversationId":99,"modelId":3}
                        """)
                .build();
        when(sessionMapper.selectById(1L)).thenReturn(session);

        assertTrue(checkpointService.loadCheckpoint(1L).isPresent());
    }

    @Test
    void resumeSession_shouldRestoreFieldsFromCheckpoint() {
        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()
                .id(1L)
                .phase("INIT")
                .checkpointJson("""
                        {"phase":"CLASSIFY_PENDING","conversationId":99,"modelId":3,
                        "partyRole":"BUYER","auditLevel":"STANDARD","auditRoleId":5,
                        "policyJson":"{}","previewOpinionJson":"[]"}
                        """)
                .build();
        when(sessionMapper.selectById(1L)).thenReturn(session);

        LegalOrchestrationCheckpointService.LegalOrchestrationCheckpoint restored =
                checkpointService.resumeSession(session);

        assertEquals("CLASSIFY_PENDING", restored.getPhase());
        verify(sessionMapper).updateById(any(LegalOrchestrationSessionDO.class));
    }

}
