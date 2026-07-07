package com.laby.module.legal.framework.agentscope.middleware;

import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummarySupport;
import com.laby.module.legal.framework.agentscope.chat.LegalContractCompactionSummaryContext;
import com.laby.module.legal.service.contract.LegalAuditCompactionSummaryService;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public class LegalAuditCompactionSummaryMiddleware implements MiddlewareBase {

    private final LegalAuditCompactionSummaryService compactionSummaryService;

    @Override
    public Flux<AgentEvent> onAgent(Agent agent, AgentInput input,
                                    Function<AgentInput, Flux<AgentEvent>> next) {
        if (!compactionSummaryService.isEnabled() || !(agent instanceof HarnessAgent harness)) {
            return next.apply(input);
        }
        Set<String> beforeHashes = AiChatCompactionSummarySupport.snapshotSummaryHashes(harness);
        return next.apply(input).doFinally(signal -> {
            RuntimeContext runtimeContext = harness.getRuntimeContext();
            LegalContractCompactionSummaryContext context = runtimeContext != null
                    ? runtimeContext.get(LegalContractCompactionSummaryContext.class) : null;
            compactionSummaryService.persistNewSummaries(harness, context, beforeHashes);
        });
    }

}
