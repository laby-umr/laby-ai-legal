package com.laby.module.legal.service.agent;

import com.laby.module.legal.service.agent.bo.LegalContractAgentPendingConfirmBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LegalContractAgentPendingConfirmStoreTest {

    private LegalContractAgentPendingConfirmStore store;

    @BeforeEach
    void setUp() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        store = new LegalContractAgentPendingConfirmStore(provider);
    }

    @Test
    void saveAndFind_roundTripViaLocalCache() {
        LegalContractAgentPendingConfirmBO snapshot = sampleSnapshot("sess-1", "confirm-abc");

        store.save(snapshot);

        Optional<LegalContractAgentPendingConfirmBO> loaded = store.find("sess-1");
        assertTrue(loaded.isPresent());
        assertEquals("sess-1", loaded.get().getSessionId());
        assertEquals(100L, loaded.get().getContractId());
        assertEquals("confirm-abc", loaded.get().getConfirmId());
        assertEquals("legal_propose_adopt_opinion", loaded.get().getToolCalls().get(0).getName());
    }

    @Test
    void remove_shouldClearSnapshot() {
        store.save(sampleSnapshot("sess-2", "confirm-xyz"));

        store.remove("sess-2");

        assertTrue(store.find("sess-2").isEmpty());
    }

    private static LegalContractAgentPendingConfirmBO sampleSnapshot(String sessionId, String confirmId) {
        LegalContractAgentPendingConfirmBO.ToolCallSnapshot toolCall =
                new LegalContractAgentPendingConfirmBO.ToolCallSnapshot();
        toolCall.setId("tool-1");
        toolCall.setName("legal_propose_adopt_opinion");
        toolCall.setInput(Map.of("opinionId", 42L));

        LegalContractAgentPendingConfirmBO snapshot = new LegalContractAgentPendingConfirmBO();
        snapshot.setSessionId(sessionId);
        snapshot.setContractId(100L);
        snapshot.setAllowProposal(true);
        snapshot.setAnswerMode("STANDARD");
        snapshot.setConfirmId(confirmId);
        snapshot.setToolCalls(List.of(toolCall));
        return snapshot;
    }

}
