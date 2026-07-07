package com.laby.module.legal.service.agent;

import com.laby.module.ai.framework.agentscope.session.InMemoryAgentScopeSessionLock;
import com.laby.module.legal.service.agent.bo.LegalContractAgentPendingConfirmBO;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LegalContractAgentRuntimeRegistryTest {

    private LegalContractAgentRuntimeRegistry registry;
    private AtomicReference<LegalContractAgentPendingConfirmBO> savedSnapshot;
    private LegalAgentSessionGuard sessionGuard;

    @BeforeEach
    void setUp() {
        registry = new LegalContractAgentRuntimeRegistry();
        savedSnapshot = new AtomicReference<>();
        LegalContractAgentPendingConfirmStore store = new LegalContractAgentPendingConfirmStore(
                mock(org.springframework.beans.factory.ObjectProvider.class)) {
            @Override
            public void save(LegalContractAgentPendingConfirmBO snapshot) {
                savedSnapshot.set(snapshot);
            }
        };
        sessionGuard = new LegalAgentSessionGuard();
        ReflectionTestUtils.setField(sessionGuard, "agentScopeSessionLock", new InMemoryAgentScopeSessionLock());
        ReflectionTestUtils.setField(registry, "pendingConfirmStore", store);
        ReflectionTestUtils.setField(registry, "agentSessionGuard", sessionGuard);
    }

    @Test
    void saveConfirmEvent_shouldPersistSnapshotAndReleaseLock() {
        HarnessAgent agent = mock(HarnessAgent.class);
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        registry.register("sess-1", agent, runtimeContext, 200L, true, "STANDARD");

        RequireUserConfirmEvent event = new RequireUserConfirmEvent("confirm-1", List.of(
                ToolUseBlock.builder()
                        .id("call-1")
                        .name("legal_propose_skip_paragraph")
                        .input(Map.of("paragraphId", "p-3"))
                        .build()));

        registry.saveConfirmEvent("sess-1", event);

        LegalContractAgentPendingConfirmBO snapshot = savedSnapshot.get();
        assertEquals("sess-1", snapshot.getSessionId());
        assertEquals(200L, snapshot.getContractId());
        assertEquals("confirm-1", snapshot.getConfirmId());
        assertEquals("legal_propose_skip_paragraph", snapshot.getToolCalls().get(0).getName());
        assertTrue(registry.isAwaitingConfirm("sess-1"));
        assertTrue(sessionGuard.tryAcquire("sess-1"));
    }

    @Test
    void remove_shouldClearAwaitingState() {
        HarnessAgent agent = mock(HarnessAgent.class);
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        registry.register("sess-2", agent, runtimeContext, 1L, false, "BRIEF");
        registry.saveConfirmEvent("sess-2", new RequireUserConfirmEvent("c-2", List.of()));

        registry.remove("sess-2");

        assertFalse(registry.isAwaitingConfirm("sess-2"));
        assertEquals(null, registry.get("sess-2"));
    }

}
