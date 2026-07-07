package com.laby.module.legal.service.contract;

import com.laby.module.legal.framework.config.LegalAuditProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 租户级审核并发控制（Redis 计数）。
 */
@Component
@RequiredArgsConstructor
public class LegalAuditConcurrencyGuard {

    private static final String KEY_PREFIX = "laby:legal:audit:running:tenant:";
    private static final Duration TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final LegalAuditProperties auditProperties;

    public boolean tryAcquire(Long tenantId) {
        if (tenantId == null) {
            return true;
        }
        String key = KEY_PREFIX + tenantId;
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, TTL);
        if (count != null && count > auditProperties.getMaxConcurrentPerTenant()) {
            redisTemplate.opsForValue().decrement(key);
            return false;
        }
        return true;
    }

    public void release(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        String key = KEY_PREFIX + tenantId;
        Long count = redisTemplate.opsForValue().decrement(key);
        if (count != null && count <= 0) {
            redisTemplate.delete(key);
        }
    }

}
