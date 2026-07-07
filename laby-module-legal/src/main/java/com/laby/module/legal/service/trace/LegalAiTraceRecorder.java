package com.laby.module.legal.service.trace;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.legal.dal.dataobject.trace.LegalAiTraceDO;
import com.laby.module.legal.dal.mysql.trace.LegalAiTraceMapper;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.enums.trace.LegalAiTraceStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 审核 / 问答链路追踪记录
 */
@Slf4j
@Component
public class LegalAiTraceRecorder {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    @Resource
    private LegalAiTraceMapper traceMapper;

    /**
     * 开始 AI 审核追踪
     */
    public TraceSession startAudit(Long contractId, int auditRound, Long modelId, String platform) {
        return start(LegalSkillPackSceneEnum.AUDIT, contractId, auditRound, modelId, platform);
    }

    /**
     * 开始合同问答追踪
     */
    public TraceSession startChat(Long contractId, Long modelId, String platform) {
        return start(LegalSkillPackSceneEnum.CHAT, contractId, null, modelId, platform);
    }

    public void completeChat(TraceSession session) {
        complete(session, 1, 0);
    }

    /**
     * 标记追踪成功
     */
    public void complete(TraceSession session, int opinionCount, int deterministicCount) {
        complete(session, opinionCount, deterministicCount, null, null, null);
    }

    /**
     * 标记追踪成功（含预览复用 / 模型 fallback 指标）
     */
    public void complete(TraceSession session, int opinionCount, int deterministicCount,
                         Integer previewReuseCount, Integer previewDedupeCount, Boolean modelFallback) {
        if (session == null) {
            return;
        }
        LegalAiTraceDO existing = traceMapper.selectByTraceId(session.traceId());
        if (existing == null) {
            return;
        }
        traceMapper.updateById(new LegalAiTraceDO()
                .setId(existing.getId())
                .setStatus(LegalAiTraceStatusEnum.SUCCESS.getCode())
                .setOpinionCount(opinionCount)
                .setDeterministicCount(deterministicCount)
                .setPreviewReuseCount(previewReuseCount)
                .setPreviewDedupeCount(previewDedupeCount)
                .setModelFallback(modelFallback)
                .setLatencyMs(System.currentTimeMillis() - session.startMs()));
    }

    /**
     * 标记追踪失败
     */
    public void fail(TraceSession session, String message) {
        if (session == null) {
            return;
        }
        LegalAiTraceDO existing = traceMapper.selectByTraceId(session.traceId());
        if (existing == null) {
            return;
        }
        traceMapper.updateById(new LegalAiTraceDO()
                .setId(existing.getId())
                .setStatus(LegalAiTraceStatusEnum.FAIL.getCode())
                .setErrorMessage(StrUtil.sub(message, 0, ERROR_MESSAGE_MAX_LENGTH))
                .setLatencyMs(System.currentTimeMillis() - session.startMs()));
    }

    private TraceSession start(LegalSkillPackSceneEnum scene, Long contractId, Integer auditRound,
                               Long modelId, String platform) {
        String traceId = IdUtil.fastSimpleUUID();
        LegalAiTraceDO trace = LegalAiTraceDO.builder()
                .traceId(traceId)
                .contractId(contractId)
                .scene(scene.getCode())
                .auditRound(auditRound)
                .modelId(modelId)
                .platform(platform)
                .status(LegalAiTraceStatusEnum.RUNNING.getCode())
                .build();
        trace.setTenantId(TenantContextHolder.getTenantId());
        traceMapper.insert(trace);
        return new TraceSession(traceId, System.currentTimeMillis());
    }

    public record TraceSession(String traceId, long startMs) {
    }

}
