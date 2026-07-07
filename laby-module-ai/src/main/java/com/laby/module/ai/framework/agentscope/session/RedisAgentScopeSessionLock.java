package com.laby.module.ai.framework.agentscope.session;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "laby.ai.agentscope.session-store", havingValue = "redis")
@RequiredArgsConstructor
public class RedisAgentScopeSessionLock implements AgentScopeSessionLock {

    private static final String LOCK_PREFIX = "laby:agentscope:lock:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return true;
        }
        String key = LOCK_PREFIX + sessionId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(30));
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void release(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            redisTemplate.delete(LOCK_PREFIX + sessionId);
        }
    }

}
