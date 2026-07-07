package com.laby.module.ai.framework.agentscope.model;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiLlmStreamEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class AgentScopeLlmClient implements AiLlmClient {

    private final AgentScopeModelConfig config;

    @SuppressWarnings("unused")
    private final Path workspaceRoot;

    @Override
    public String call(AiLlmRequest request) {
        GenerateOptions options = GenerateOptions.builder().stream(false).build();
        Model model = AgentScopeModelFactory.buildChatModel(config, request);
        List<Msg> messages = buildCallMessages(request);
        ChatResponse response = model.stream(messages, Collections.emptyList(), options).blockLast();
        String text = extractChatResponseText(response);
        if (StrUtil.isBlank(text)) {
            throw new IllegalStateException("LLM 返回内容为空");
        }
        return text;
    }

    @Override
    public Flux<AiLlmStreamEvent> stream(AiLlmRequest request) {
        GenerateOptions options = GenerateOptions.builder().stream(true).build();
        Model model = AgentScopeModelFactory.buildChatModel(config, request);
        List<Msg> messages = buildCallMessages(request);
        AtomicReference<String> contentAccum = new AtomicReference<>("");
        AtomicReference<String> reasoningAccum = new AtomicReference<>("");
        return model.stream(messages, Collections.emptyList(), options)
                .concatMap(chunk -> Flux.fromIterable(mapChatResponseChunk(chunk, contentAccum, reasoningAccum)))
                .onErrorResume(ex -> Flux.just(errorEvent(resolveStreamErrorMessage(ex))))
                .concatWith(Flux.just(new AiLlmStreamEvent().setType(AiLlmStreamEvent.Type.DONE)));
    }

    private static List<AiLlmStreamEvent> mapChatResponseChunk(ChatResponse chunk,
                                                               AtomicReference<String> contentAccum,
                                                               AtomicReference<String> reasoningAccum) {
        if (chunk == null || chunk.getContent() == null) {
            return List.of();
        }
        List<AiLlmStreamEvent> events = new ArrayList<>();
        for (ContentBlock block : chunk.getContent()) {
            if (block instanceof TextBlock textBlock && StrUtil.isNotBlank(textBlock.getText())) {
                appendDeltaEvent(events, contentAccum, textBlock.getText(), AiLlmStreamEvent.Type.CONTENT);
            } else if (block instanceof ThinkingBlock thinkingBlock) {
                String thinking = resolveThinkingText(thinkingBlock);
                if (StrUtil.isNotBlank(thinking)) {
                    appendDeltaEvent(events, reasoningAccum, thinking, AiLlmStreamEvent.Type.REASONING);
                }
            }
        }
        return events;
    }

    private static void appendDeltaEvent(List<AiLlmStreamEvent> events, AtomicReference<String> accum,
                                         String current, AiLlmStreamEvent.Type type) {
        String delta = extractDelta(accum, current);
        if (StrUtil.isNotBlank(delta)) {
            events.add(new AiLlmStreamEvent().setType(type).setDelta(delta));
        }
    }

    /**
     * AgentScope 流式 chunk 可能是纯增量，也可能是最终累积全文；统一转成增量 delta。
     */
    private static String extractDelta(AtomicReference<String> accum, String current) {
        String previous = accum.get();
        if (StrUtil.isBlank(previous)) {
            accum.set(current);
            return current;
        }
        if (current.equals(previous)) {
            return null;
        }
        if (current.startsWith(previous)) {
            String delta = current.substring(previous.length());
            accum.set(current);
            return delta;
        }
        accum.set(previous + current);
        return current;
    }

    private static String resolveThinkingText(ThinkingBlock thinkingBlock) {
        return thinkingBlock == null ? null : thinkingBlock.getThinking();
    }

    private List<Msg> buildCallMessages(AiLlmRequest request) {
        return AiMessageConverter.toAgentScopeMessages(request.getMessages());
    }

    private static String extractChatResponseText(ChatResponse response) {
        if (response == null || response.getContent() == null) {
            return null;
        }
        StringBuilder textBuilder = new StringBuilder();
        StringBuilder thinkingBuilder = new StringBuilder();
        for (ContentBlock block : response.getContent()) {
            if (block instanceof TextBlock textBlock && StrUtil.isNotBlank(textBlock.getText())) {
                textBuilder.append(textBlock.getText());
            } else if (block instanceof ThinkingBlock thinkingBlock) {
                String thinking = resolveThinkingText(thinkingBlock);
                if (StrUtil.isNotBlank(thinking)) {
                    thinkingBuilder.append(thinking);
                }
            }
        }
        String text = textBuilder.toString().trim();
        if (StrUtil.isNotBlank(text)) {
            return text;
        }
        return thinkingBuilder.toString().trim();
    }

    private static AiLlmStreamEvent errorEvent(String message) {
        return new AiLlmStreamEvent()
                .setType(AiLlmStreamEvent.Type.ERROR)
                .setErrorMessage(message);
    }

    private static String resolveStreamErrorMessage(Throwable ex) {
        String message = StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
        if (message.contains("Connection reset") || message.contains("Retries exhausted")) {
            return "模型服务连接中断，请检查 API 地址/密钥或稍后重试";
        }
        return "模型调用失败：" + StrUtil.sub(message, 0, 200);
    }

}
