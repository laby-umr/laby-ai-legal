package com.laby.module.legal.service.orchestration;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationFileItemMapper;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;
import com.laby.module.legal.enums.orchestration.LegalOrchestrationPhaseEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalOrchestrationSessionServiceImplTest {

    @InjectMocks
    private LegalOrchestrationSessionServiceImpl sessionService;

    @Mock
    private LegalOrchestrationSessionMapper sessionMapper;
    @Mock
    private LegalOrchestrationFileItemMapper fileItemMapper;
    @Mock
    private LegalOrchestrationCheckpointService checkpointService;

    @Test
    void getOrCreateSession_shouldReturnExisting() {
        LegalOrchestrationSessionDO existing = LegalOrchestrationSessionDO.builder()
                .id(1L).conversationId(99L).userId(2L).phase(LegalOrchestrationPhaseEnum.INIT.getPhase()).build();
        when(sessionMapper.selectByConversationId(99L)).thenReturn(existing);

        LegalOrchestrationSessionDO result = sessionService.getOrCreateSession(99L, 2L, 1L);

        assertEquals(1L, result.getId());
        verify(sessionMapper, never()).insert(any(LegalOrchestrationSessionDO.class));
    }

    @Test
    void getOrCreateSession_shouldBackfillModelIdWhenMissing() {
        LegalOrchestrationSessionDO existing = LegalOrchestrationSessionDO.builder()
                .id(1L).conversationId(99L).userId(2L).phase(LegalOrchestrationPhaseEnum.INIT.getPhase()).build();
        when(sessionMapper.selectByConversationId(99L)).thenReturn(existing);

        LegalOrchestrationSessionDO result = sessionService.getOrCreateSession(99L, 2L, 7L);

        assertEquals(7L, result.getModelId());
        ArgumentCaptor<LegalOrchestrationSessionDO> captor =
                ArgumentCaptor.forClass(LegalOrchestrationSessionDO.class);
        verify(sessionMapper).updateById(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(7L, captor.getValue().getModelId());
    }

    @Test
    void getOrCreateSession_shouldInsertWhenMissing() {
        when(sessionMapper.selectByConversationId(99L)).thenReturn(null);

        LegalOrchestrationSessionDO result = sessionService.getOrCreateSession(99L, 2L, 1L);

        assertNotNull(result);
        verify(sessionMapper).insert(any(LegalOrchestrationSessionDO.class));
    }

}
