package com.laby.module.legal.framework.agentscope.permission;

import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalAgentPermissionContextFactoryTest {

    @Test
    void build_whenAllowProposal_shouldAllowReadonlyAndProposeTools() {
        PermissionContextState ctx = LegalAgentPermissionContextFactory.build(true);

        assertEquals(PermissionMode.DEFAULT, ctx.getMode());
        assertTrue(hasBehavior(ctx, "legal_get_contract_meta", PermissionBehavior.ALLOW));
        assertTrue(hasBehavior(ctx, "legal_propose_adopt_opinion", PermissionBehavior.ALLOW));
        assertTrue(hasBehavior(ctx, "legal_adopt_opinion", PermissionBehavior.ALLOW));
        assertTrue(hasBehavior(ctx, "legal_batch_adopt_pending_opinions", PermissionBehavior.ALLOW));
        assertTrue(hasBehavior(ctx, "legal_propose_skip_paragraph", PermissionBehavior.ALLOW));
        assertTrue(hasBehavior(ctx, "session_search", PermissionBehavior.ALLOW));
    }

    @Test
    void build_whenProposalDisabled_shouldExploreAndDenyPropose() {
        PermissionContextState ctx = LegalAgentPermissionContextFactory.build(false);

        assertEquals(PermissionMode.EXPLORE, ctx.getMode());
        assertTrue(hasBehavior(ctx, "legal_propose_adopt_opinion", PermissionBehavior.DENY));
    }

    private static boolean hasBehavior(PermissionContextState ctx, String toolName,
                                       PermissionBehavior behavior) {
        return switch (behavior) {
            case ALLOW -> ctx.getAllowRules().containsKey(toolName);
            case ASK -> ctx.getAskRules().containsKey(toolName);
            case DENY -> ctx.getDenyRules().containsKey(toolName);
            default -> false;
        };
    }

}
