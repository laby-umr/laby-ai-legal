package com.laby.module.legal.service.contract;

import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalAiAuditProgressRespVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 版 AI 审核进度（多实例共享，供前端轮询）。
 */
@Service
@RequiredArgsConstructor
public class LegalAiAuditProgressService {

    private static final String KEY_PREFIX = "laby:legal:audit:progress:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public void start(Long contractId, int auditRound, int totalBatches) {
        Progress progress = new Progress();
        progress.setStatus("RUNNING");
        progress.setAuditRound(auditRound);
        progress.setBatchIndex(0);
        progress.setTotalBatches(totalBatches);
        progress.setMessage("AI 审核已开始");
        progress.setReasoningContent("");
        save(contractId, progress);
    }

    public void onBatch(Long contractId, int batchIndex, int totalBatches, String message) {
        Progress progress = load(contractId);
        if (progress == null) {
            return;
        }
        progress.setStatus("RUNNING");
        progress.setBatchIndex(batchIndex);
        progress.setTotalBatches(totalBatches);
        progress.setMessage(message);
        save(contractId, progress);
    }

    public void appendReasoning(Long contractId, String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }
        Progress progress = load(contractId);
        if (progress == null) {
            return;
        }
        String existing = progress.getReasoningContent() == null ? "" : progress.getReasoningContent();
        progress.setReasoningContent(existing + chunk);
        save(contractId, progress);
    }

    public void complete(Long contractId, String message) {
        Progress progress = load(contractId);
        if (progress == null) {
            progress = new Progress();
        }
        progress.setStatus("COMPLETED");
        progress.setMessage(message == null || message.isBlank() ? "AI 审核已完成" : message);
        save(contractId, progress);
    }

    public void fail(Long contractId, String message) {
        Progress progress = load(contractId);
        if (progress == null) {
            progress = new Progress();
        }
        progress.setStatus("FAILED");
        progress.setMessage(message == null || message.isBlank() ? "AI 审核失败" : message);
        save(contractId, progress);
    }

    public void clear(Long contractId) {
        if (contractId != null) {
            redisTemplate.delete(key(contractId));
        }
    }

    public LegalAiAuditProgressRespVO get(Long contractId) {
        Progress progress = load(contractId);
        if (progress == null) {
            LegalAiAuditProgressRespVO idle = new LegalAiAuditProgressRespVO();
            idle.setStatus("IDLE");
            return idle;
        }
        LegalAiAuditProgressRespVO vo = new LegalAiAuditProgressRespVO();
        vo.setStatus(progress.getStatus());
        vo.setAuditRound(progress.getAuditRound());
        vo.setBatchIndex(progress.getBatchIndex());
        vo.setTotalBatches(progress.getTotalBatches());
        vo.setMessage(progress.getMessage());
        vo.setReasoningContent(progress.getReasoningContent());
        return vo;
    }

    private void save(Long contractId, Progress progress) {
        redisTemplate.opsForValue().set(key(contractId), JsonUtils.toJsonString(progress), TTL);
    }

    private Progress load(Long contractId) {
        String json = redisTemplate.opsForValue().get(key(contractId));
        if (json == null || json.isBlank()) {
            return null;
        }
        return JsonUtils.parseObject(json, Progress.class);
    }

    private static String key(Long contractId) {
        return KEY_PREFIX + contractId;
    }

    @Data
    private static class Progress {
        private String status;
        private Integer auditRound;
        private Integer batchIndex;
        private Integer totalBatches;
        private String message;
        private String reasoningContent;
    }

}
