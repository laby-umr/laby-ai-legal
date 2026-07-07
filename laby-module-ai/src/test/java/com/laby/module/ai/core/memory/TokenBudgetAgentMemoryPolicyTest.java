package com.laby.module.ai.core.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenBudgetAgentMemoryPolicyTest {

    private final TokenBudgetAgentMemoryPolicy policy = new TokenBudgetAgentMemoryPolicy();

    @Test
    void trimTail_shouldKeepLatestWithinTokenBudget() {
        List<String> items = List.of("aaaa", "bbbb", "cccc", "dddd");
        List<String> trimmed = policy.trimTail(items, 10, 0, s -> s, String::length);
        assertEquals(List.of("cccc", "dddd"), trimmed);
    }

    @Test
    void trimTail_shouldRespectMaxItems() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        List<String> trimmed = policy.trimTail(items, 0, 2, s -> s, String::length);
        assertEquals(List.of("d", "e"), trimmed);
    }

}
