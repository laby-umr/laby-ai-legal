package com.laby.module.legal.service.contract;

import com.laby.module.legal.framework.config.LegalAuditProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalAuditConcurrencyGuardTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final LegalAuditProperties auditProperties = new LegalAuditProperties();

    private LegalAuditConcurrencyGuard guard;

    @BeforeEach
    void setUp() {
        auditProperties.setMaxConcurrentPerTenant(2);
        guard = new LegalAuditConcurrencyGuard(redisTemplate, auditProperties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void tryAcquire_withinLimit_returnsTrue() {
        when(valueOperations.increment("laby:legal:audit:running:tenant:1")).thenReturn(1L);

        assertTrue(guard.tryAcquire(1L));
        verify(redisTemplate).expire(eq("laby:legal:audit:running:tenant:1"), any(Duration.class));
    }

    @Test
    void tryAcquire_exceedsLimit_returnsFalseAndRollsBack() {
        when(valueOperations.increment("laby:legal:audit:running:tenant:1")).thenReturn(3L);

        assertFalse(guard.tryAcquire(1L));
        verify(valueOperations).decrement("laby:legal:audit:running:tenant:1");
    }

    @Test
    void release_whenLastWorker_clearsKey() {
        when(valueOperations.decrement("laby:legal:audit:running:tenant:1")).thenReturn(0L);

        guard.release(1L);

        verify(redisTemplate).delete("laby:legal:audit:running:tenant:1");
    }

}
