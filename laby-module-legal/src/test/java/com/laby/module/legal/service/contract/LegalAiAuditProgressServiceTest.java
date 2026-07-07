package com.laby.module.legal.service.contract;

import com.laby.module.legal.controller.admin.contract.vo.LegalAiAuditProgressRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalAiAuditProgressServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LegalAiAuditProgressService progressService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void start_shouldWriteRunningStatusToRedis() {
        progressService.start(42L, 1, 8);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("laby:legal:audit:progress:42"), jsonCaptor.capture(), eq(Duration.ofHours(24)));
        assertTrue(jsonCaptor.getValue().contains("RUNNING"));
        assertTrue(jsonCaptor.getValue().contains("\"totalBatches\":8"));
    }

    @Test
    void get_whenMissing_returnsIdle() {
        when(valueOperations.get("laby:legal:audit:progress:99")).thenReturn(null);

        LegalAiAuditProgressRespVO vo = progressService.get(99L);

        assertEquals("IDLE", vo.getStatus());
    }

    @Test
    void get_whenPresent_deserializesProgress() {
        when(valueOperations.get("laby:legal:audit:progress:7")).thenReturn("""
                {"status":"RUNNING","auditRound":1,"batchIndex":2,"totalBatches":5,"message":"批处理中","reasoningContent":""}
                """);

        LegalAiAuditProgressRespVO vo = progressService.get(7L);

        assertEquals("RUNNING", vo.getStatus());
        assertEquals(2, vo.getBatchIndex());
        assertEquals(5, vo.getTotalBatches());
    }

    @Test
    void clear_shouldDeleteRedisKey() {
        progressService.clear(11L);
        verify(redisTemplate).delete("laby:legal:audit:progress:11");
    }

}
