package com.laby.module.ai.framework.agentscope.chat.middleware;

import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummaryContext;
import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummarySupport;
import com.laby.module.ai.service.chat.AiChatCompactionSummaryService;
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

/**
 * Agent 回合结束后，将 Harness Compaction 摘要持久化到 DB。
 */
@RequiredArgsConstructor
public class AiChatCompactionSummaryMiddleware implements MiddlewareBase {

    private final AiChatCompactionSummaryService compactionSummaryService;

    @Override
    public Flux<AgentEvent> onAgent(Agent agent, AgentInput input,
                                    Function<AgentInput, Flux<AgentEvent>> next) {
        if (!compactionSummaryService.isEnabled() || !(agent instanceof HarnessAgent harness)) {
            return next.apply(input);
        }
        Set<String> beforeHashes = AiChatCompactionSummarySupport.snapshotSummaryHashes(harness);
        return next.apply(input).doFinally(signal -> {
            RuntimeContext runtimeContext = harness.getRuntimeContext();
            AiChatCompactionSummaryContext context = runtimeContext != null
                    ? runtimeContext.get(AiChatCompactionSummaryContext.class) : null;
            compactionSummaryService.persistNewSummaries(harness, context, beforeHashes);
        });
    }

}
