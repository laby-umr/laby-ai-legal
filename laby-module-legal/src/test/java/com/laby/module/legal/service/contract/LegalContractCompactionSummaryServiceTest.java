package com.laby.module.legal.service.contract;

import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.legal.dal.mysql.contract.LegalContractChatMessageMapper;
import com.laby.module.legal.framework.agentscope.chat.LegalContractCompactionSummaryContext;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalContractCompactionSummaryServiceTest {

    @Mock
    private AgentScopeProperties agentScopeProperties;
    @Mock
    private LegalContractChatMessageMapper chatMessageMapper;
    @Mock
    private HarnessAgent harnessAgent;
    @InjectMocks
    private LegalContractCompactionSummaryService compactionSummaryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(compactionSummaryService, "agentScopeProperties", agentScopeProperties);
        ReflectionTestUtils.setField(compactionSummaryService, "chatMessageMapper", chatMessageMapper);
    }

    @Test
    void isEnabled_shouldFollowAgentScopeProperty() {
        when(agentScopeProperties.isCompactionSummaryPersist()).thenReturn(true);
        assertTrue(compactionSummaryService.isEnabled());
        when(agentScopeProperties.isCompactionSummaryPersist()).thenReturn(false);
        assertFalse(compactionSummaryService.isEnabled());
    }

    @Test
    void persistNewSummaries_shouldSkipWhenDisabled() {
        when(agentScopeProperties.isCompactionSummaryPersist()).thenReturn(false);
        compactionSummaryService.persistNewSummaries(harnessAgent,
                LegalContractCompactionSummaryContext.of(1L, 2L, "sess"), Set.of());
        verifyNoInteractions(chatMessageMapper);
    }

}
