package com.laby.module.legal.service.contract;

import com.laby.module.ai.core.memory.TokenBudgetAgentMemoryPolicy;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.framework.config.LegalChatMemoryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegalContractChatHistoryHelperTest {

    private final LegalContractChatHistoryHelper helper = new LegalContractChatHistoryHelper();

    @BeforeEach
    void setUp() {
        LegalChatMemoryProperties properties = new LegalChatMemoryProperties();
        properties.setMaxHistoryTurns(8);
        properties.setHistoryTokenBudget(20);
        properties.setMaxTurnChars(100);
        ReflectionTestUtils.setField(helper, "memoryProperties", properties);
        ReflectionTestUtils.setField(helper, "agentMemoryPolicy", new TokenBudgetAgentMemoryPolicy());
    }

    @Test
    void filterAndTrim_shouldSkipSummaryMessages() {
        List<LegalContractChatMessageDO> history = List.of(
                message("summary", "compaction summary text"),
                message("user", "question"),
                message("assistant", "answer"));

        List<LegalContractChatMessageDO> trimmed = helper.filterAndTrim(history);

        assertEquals(2, trimmed.size());
        assertEquals("question", trimmed.get(0).getContent());
    }

    @Test
    void filterAndTrim_shouldSkipNoiseAndRespectTokenBudget() {
        LegalContractChatMessageDO skip = message("assistant", "问答失败，请重试");
        List<LegalContractChatMessageDO> history = List.of(
                message("user", "old message with many tokens here"),
                skip,
                message("user", "latest question"),
                message("assistant", "latest answer"));

        List<LegalContractChatMessageDO> trimmed = helper.filterAndTrim(history);

        assertEquals(3, trimmed.size());
        assertEquals("latest question", trimmed.get(1).getContent());
        assertEquals("latest answer", trimmed.get(2).getContent());
    }

    private static LegalContractChatMessageDO message(String type, String content) {
        return LegalContractChatMessageDO.builder().type(type).content(content).build();
    }

}
