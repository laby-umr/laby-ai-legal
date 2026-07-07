package com.laby.module.legal.service.agent;

import com.laby.module.ai.framework.agentscope.session.InMemoryAgentScopeSessionLock;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractAgentConfirmReqVO;
import com.laby.module.legal.service.agent.bo.LegalContractAgentPendingConfirmBO;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalContractAgentResumeServiceTest {

    @Mock
    private LegalContractAgentRuntimeRegistry runtimeRegistry;
    @Mock
    private LegalContractAgentPendingConfirmStore pendingConfirmStore;
    @Mock
    private LegalContractAgentRebuildService rebuildService;

    private LegalContractAgentResumeService resumeService;

    private LegalContractAgentPendingConfirmBO snapshot;

    @BeforeEach
    void setUp() {
        resumeService = new LegalContractAgentResumeService();
        ReflectionTestUtils.setField(resumeService, "runtimeRegistry", runtimeRegistry);
        ReflectionTestUtils.setField(resumeService, "pendingConfirmStore", pendingConfirmStore);
        ReflectionTestUtils.setField(resumeService, "rebuildService", rebuildService);
        LegalAgentSessionGuard guard = new LegalAgentSessionGuard();
        ReflectionTestUtils.setField(guard, "agentScopeSessionLock", new InMemoryAgentScopeSessionLock());
        ReflectionTestUtils.setField(resumeService, "agentSessionGuard", guard);

        snapshot = new LegalContractAgentPendingConfirmBO();
        snapshot.setSessionId("sess-cross");
        snapshot.setContractId(1L);
        snapshot.setAllowProposal(true);
        snapshot.setAnswerMode("STANDARD");
        snapshot.setConfirmId("confirm-cross");
        LegalContractAgentPendingConfirmBO.ToolCallSnapshot toolCall =
                new LegalContractAgentPendingConfirmBO.ToolCallSnapshot();
        toolCall.setId("tc-1");
        toolCall.setName("legal_propose_adopt_opinion");
        toolCall.setInput(Map.of("opinionId", 9L));
        snapshot.setToolCalls(List.of(toolCall));
    }

    @Test
    void resolvePendingRun_shouldUseLocalPendingRunWhenPresent() {
        LegalContractAgentRuntimeRegistry.PendingRun local = pendingRunWithConfirm("confirm-local");
        when(runtimeRegistry.get("sess-local")).thenReturn(local);

        LegalContractAgentRuntimeRegistry.PendingRun resolved = invokeResolvePendingRun(
                confirmReq("sess-local", "confirm-local", true));

        assertSame(local, resolved);
        verify(pendingConfirmStore, never()).find("sess-local");
        verify(rebuildService, never()).rebuild(any());
    }

    @Test
    void resolvePendingRun_shouldRebuildFromRedisWhenLocalMissing() {
        LegalContractAgentRuntimeRegistry.PendingRun rebuilt = pendingRunWithConfirm("confirm-cross");
        rebuilt.setSessionId("sess-cross");
        when(runtimeRegistry.get("sess-cross")).thenReturn(null);
        when(pendingConfirmStore.find("sess-cross")).thenReturn(Optional.of(snapshot));
        when(rebuildService.rebuild(snapshot)).thenReturn(rebuilt);

        LegalContractAgentRuntimeRegistry.PendingRun resolved = invokeResolvePendingRun(
                confirmReq("sess-cross", "confirm-cross", true));

        assertSame(rebuilt, resolved);
        verify(rebuildService).rebuild(snapshot);
        verify(runtimeRegistry).register("sess-cross", rebuilt.getAgent(), rebuilt.getRuntimeContext(),
                rebuilt.getContractId(), rebuilt.isAllowProposal(), rebuilt.getAnswerMode());
        verify(runtimeRegistry).saveConfirmEvent("sess-cross", rebuilt.getConfirmEvent());
    }

    private LegalContractAgentRuntimeRegistry.PendingRun invokeResolvePendingRun(
            LegalContractAgentConfirmReqVO reqVO) {
        return ReflectionTestUtils.invokeMethod(resumeService, "resolvePendingRun", reqVO);
    }

    private static LegalContractAgentConfirmReqVO confirmReq(String sessionId, String confirmId, boolean approved) {
        LegalContractAgentConfirmReqVO reqVO = new LegalContractAgentConfirmReqVO();
        reqVO.setSessionId(sessionId);
        reqVO.setConfirmId(confirmId);
        reqVO.setApproved(approved);
        return reqVO;
    }

    private static LegalContractAgentRuntimeRegistry.PendingRun pendingRunWithConfirm(String confirmId) {
        LegalContractAgentRuntimeRegistry.PendingRun pending = new LegalContractAgentRuntimeRegistry.PendingRun();
        pending.setSessionId("ignored");
        pending.setAgent(mock(HarnessAgent.class));
        pending.setRuntimeContext(mock(RuntimeContext.class));
        pending.setConfirmEvent(new RequireUserConfirmEvent(confirmId, List.of()));
        pending.setContractId(1L);
        pending.setAllowProposal(true);
        pending.setAnswerMode("STANDARD");
        assertNotNull(pending.getAgent());
        return pending;
    }

}
