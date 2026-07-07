package com.laby.module.legal.service.orchestration;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laby.module.legal.framework.config.LegalOrchestrationProperties;
import com.laby.module.legal.tool.orchestration.LegalOrchestrationToolRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 编排 Tool 上下文（多实例共享）。
 */
@Slf4j
public class RedisLegalOrchestrationToolContextStore implements LegalOrchestrationToolContextStore {

    private static final String KEY_PREFIX = "laby:legal:orch-tool-ctx:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final InMemoryLegalOrchestrationToolContextStore localFallback = new InMemoryLegalOrchestrationToolContextStore();

    public RedisLegalOrchestrationToolContextStore(StringRedisTemplate redisTemplate,
                                                   ObjectMapper objectMapper,
                                                   LegalOrchestrationProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofHours(Math.max(properties.getToolContextTtlHours(), 1));
    }

    @Override
    public void bindConversation(Long conversationId, LegalOrchestrationToolRuntimeContext context) {
        if (conversationId == null || context == null) {
            return;
        }
        localFallback.bindConversation(conversationId, context);
        try {
            String json = objectMapper.writeValueAsString(context);
            redisTemplate.opsForValue().set(buildKey(conversationId), json, ttl);
        } catch (JsonProcessingException ex) {
            log.warn("[bindConversation] 序列化编排上下文失败 conversationId={}: {}", conversationId, ex.getMessage());
        }
    }

    @Override
    public LegalOrchestrationToolRuntimeContext getByConversationId(Long conversationId) {
        if (conversationId == null) {
            return null;
        }
        LegalOrchestrationToolRuntimeContext local = localFallback.getByConversationId(conversationId);
        if (local != null) {
            return local;
        }
        return readFromRedis(conversationId);
    }

    @Override
    public LegalOrchestrationToolRuntimeContext getBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        LegalOrchestrationToolRuntimeContext local = localFallback.getBySessionId(sessionId);
        if (local != null) {
            return local;
        }
        try {
            return readFromRedis(Long.parseLong(sessionId));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LegalOrchestrationToolRuntimeContext readFromRedis(Long conversationId) {
        String json = redisTemplate.opsForValue().get(buildKey(conversationId));
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            LegalOrchestrationToolRuntimeContext context = objectMapper.readValue(
                    json, LegalOrchestrationToolRuntimeContext.class);
            localFallback.bindConversation(conversationId, context);
            return context;
        } catch (JsonProcessingException ex) {
            log.warn("[readFromRedis] 反序列化失败 conversationId={}: {}", conversationId, ex.getMessage());
            return null;
        }
    }

    private static String buildKey(Long conversationId) {
        return KEY_PREFIX + conversationId;
    }

}
