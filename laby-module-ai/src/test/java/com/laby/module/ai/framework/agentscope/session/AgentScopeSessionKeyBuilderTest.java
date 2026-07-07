package com.laby.module.ai.framework.agentscope.session;

import io.agentscope.core.state.SessionKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentScopeSessionKeyBuilderTest {

    @Test
    void legalContract_shouldJoinPrefixScopeAndSession() {
        SessionKey key = AgentScopeSessionKeyBuilder.legalContract("as:", 42L, "abc-session");

        assertEquals("as_legal_42_abc-session", key.toIdentifier());
    }

    @Test
    void aiChat_shouldNormalizePrefixWithoutColon() {
        SessionKey key = AgentScopeSessionKeyBuilder.aiChat("laby", "conv-9");

        assertEquals("laby_chat_conv-9", key.toIdentifier());
    }

}
