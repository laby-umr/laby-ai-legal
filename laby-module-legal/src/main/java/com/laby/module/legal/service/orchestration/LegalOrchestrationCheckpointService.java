package com.laby.module.legal.service.orchestration;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_CHECKPOINT_NOT_FOUND;

/**
 * 编排阶段 Checkpoint（LangGraph 思路：阶段快照可恢复）。
 */
@Service
public class LegalOrchestrationCheckpointService {

    @Resource
    private LegalOrchestrationSessionMapper sessionMapper;

    public void savePhaseCheckpoint(LegalOrchestrationSessionDO session, String phase) {
        if (session == null || session.getId() == null || StrUtil.isBlank(phase)) {
            return;
        }
        LegalOrchestrationCheckpoint checkpoint = new LegalOrchestrationCheckpoint()
                .setPhase(phase)
                .setSavedAt(LocalDateTime.now())
                .setConversationId(session.getConversationId())
                .setModelId(session.getModelId())
                .setPartyRole(session.getPartyRole())
                .setAuditLevel(session.getAuditLevel())
                .setAuditRoleId(session.getAuditRoleId())
                .setPolicyJson(session.getPolicyJson())
                .setPreviewOpinionJson(session.getPreviewOpinionJson());
        sessionMapper.updateById(new LegalOrchestrationSessionDO()
                .setId(session.getId())
                .setPhase(phase)
                .setCheckpointJson(JsonUtils.toJsonString(checkpoint)));
    }

    public Optional<LegalOrchestrationCheckpoint> loadCheckpoint(Long sessionId) {
        LegalOrchestrationSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || StrUtil.isBlank(session.getCheckpointJson())) {
            return Optional.empty();
        }
        return Optional.ofNullable(JsonUtils.parseObject(session.getCheckpointJson(), LegalOrchestrationCheckpoint.class));
    }

    /**
     * 从最近 Checkpoint 恢复会话编排状态（阶段、策略、预览快照等）。
     */
    public LegalOrchestrationCheckpoint resumeSession(LegalOrchestrationSessionDO session) {
        LegalOrchestrationCheckpoint checkpoint = loadCheckpoint(session.getId())
                .orElseThrow(() -> exception(ORCHESTRATION_CHECKPOINT_NOT_FOUND));
        sessionMapper.updateById(new LegalOrchestrationSessionDO()
                .setId(session.getId())
                .setPhase(checkpoint.getPhase())
                .setModelId(checkpoint.getModelId())
                .setPartyRole(checkpoint.getPartyRole())
                .setAuditLevel(checkpoint.getAuditLevel())
                .setAuditRoleId(checkpoint.getAuditRoleId())
                .setPolicyJson(checkpoint.getPolicyJson())
                .setPreviewOpinionJson(checkpoint.getPreviewOpinionJson()));
        return checkpoint;
    }

    @Data
    public static class LegalOrchestrationCheckpoint {
        private String phase;
        private LocalDateTime savedAt;
        private Long conversationId;
        private Long modelId;
        private String partyRole;
        private String auditLevel;
        private Long auditRoleId;
        private String policyJson;
        private String previewOpinionJson;
    }

}
