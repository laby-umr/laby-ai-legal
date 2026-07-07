package com.laby.module.ai.core.llm;

import reactor.core.publisher.Flux;

public interface AiLlmClient {
    String call(AiLlmRequest request);
    Flux<AiLlmStreamEvent> stream(AiLlmRequest request);
}
