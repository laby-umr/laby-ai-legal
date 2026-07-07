package com.laby.module.ai.framework.agentscope.chat;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.state.AgentState;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.ConversationCompactor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class AiChatCompactionSummarySupportTest {

    @Test
    void extractSummaryTexts_shouldReadCompactionSummaryMessage() {
        HarnessAgent agent = Mockito.mock(HarnessAgent.class);
        AgentState state = Mockito.mock(AgentState.class);
        List<Msg> messages = new ArrayList<>();
        messages.add(Msg.builder()
                .role(MsgRole.USER)
                .name(ConversationCompactor.SUMMARY_MSG_NAME)
                .content(TextBlock.builder().text("summary body").build())
                .build());
        when(agent.getAgentState()).thenReturn(state);
        when(state.contextMutable()).thenReturn(messages);

        List<String> summaries = AiChatCompactionSummarySupport.extractSummaryTexts(agent);

        assertEquals(List.of("summary body"), summaries);
    }

}
