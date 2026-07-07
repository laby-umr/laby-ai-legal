package com.laby.module.legal.tool.orchestration;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalOrchestrationToolSupportTest {

    private static final Long CONVERSATION_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 5L;

    @Mock
    private LegalOrchestrationSessionService sessionService;

    @Test
    void resolveOrchestrationSession_shouldUseConversationWhenHintIsConversationId() {
        LegalOrchestrationSessionDO session = session(CONVERSATION_ID, SESSION_ID);
        when(sessionService.getByConversationId(CONVERSATION_ID)).thenReturn(session);

        LegalOrchestrationSessionDO resolved = LegalOrchestrationToolSupport.resolveOrchestrationSession(
                sessionService, CONVERSATION_ID, context());

        assertEquals(SESSION_ID, resolved.getId());
    }

    @Test
    void resolveOrchestrationSession_shouldUseSessionIdWhenHintMatches() {
        LegalOrchestrationSessionDO session = session(CONVERSATION_ID, SESSION_ID);
        when(sessionService.getByConversationId(CONVERSATION_ID)).thenReturn(session);

        LegalOrchestrationSessionDO resolved = LegalOrchestrationToolSupport.resolveOrchestrationSession(
                sessionService, SESSION_ID, context());

        assertEquals(SESSION_ID, resolved.getId());
    }

    @Test
    void resolveOrchestrationSession_shouldGetOrCreateWhenHintMissing() {
        LegalOrchestrationSessionDO session = session(CONVERSATION_ID, SESSION_ID);
        when(sessionService.getByConversationId(CONVERSATION_ID)).thenReturn(session);

        LegalOrchestrationSessionDO resolved = LegalOrchestrationToolSupport.resolveOrchestrationSession(
                sessionService, null, context());

        assertEquals(SESSION_ID, resolved.getId());
        verify(sessionService).getByConversationId(CONVERSATION_ID);
    }

    private static LegalOrchestrationToolRuntimeContext context() {
        return LegalOrchestrationToolRuntimeContext.builder()
                .conversationId(CONVERSATION_ID)
                .userId(USER_ID)
                .modelId(7L)
                .build();
    }

    private static LegalOrchestrationSessionDO session(Long conversationId, Long sessionId) {
        return LegalOrchestrationSessionDO.builder()
                .id(sessionId)
                .conversationId(conversationId)
                .userId(USER_ID)
                .build();
    }

}
