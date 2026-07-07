package com.laby.module.legal.controller.admin.orchestration;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.controller.admin.orchestration.vo.LegalOrchestrationCheckpointRespVO;
import com.laby.module.legal.controller.admin.orchestration.vo.LegalOrchestrationFileItemRespVO;
import com.laby.module.legal.controller.admin.orchestration.vo.LegalOrchestrationSessionRespVO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationCheckpointService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationPreviewAuditService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationAuditPreviewSnapshotBO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.laby.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 法务 AI 编排会话")
@RestController
@RequestMapping("/legal/orchestration/session")
@Validated
public class LegalOrchestrationSessionController {

    @Resource
    private LegalOrchestrationSessionService sessionService;
    @Resource
    private AiModelService aiModelService;
    @Resource
    private LegalOrchestrationPreviewAuditService previewAuditService;
    @Resource
    private LegalOrchestrationCheckpointService checkpointService;

    @GetMapping("/get")
    @Operation(summary = "按对话编号获取编排会话（未创建时返回 null）")
    @PreAuthorize("@ss.hasPermission('legal:orchestration:query') or @ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalOrchestrationSessionRespVO> getSession(
            @Parameter(description = "AI 对话编号", required = true) @RequestParam("conversationId") Long conversationId) {
        LegalOrchestrationSessionDO session = resolveOwnedSession(conversationId);
        if (session == null) {
            return success(null);
        }
        LegalOrchestrationSessionRespVO resp = BeanUtils.toBean(session, LegalOrchestrationSessionRespVO.class);
        if (session.getModelId() != null) {
            AiModelDO model = aiModelService.getModel(session.getModelId());
            if (model != null) {
                resp.setModelName(model.getName());
            }
        }
        enrichPreviewSummary(resp, session.getId());
        checkpointService.loadCheckpoint(session.getId()).ifPresent(checkpoint -> {
            resp.setCheckpointSavedAt(checkpoint.getSavedAt());
            resp.setCheckpointPhase(checkpoint.getPhase());
        });
        resp.setFileItems(BeanUtils.toBean(sessionService.listFileItems(session.getId()),
                LegalOrchestrationFileItemRespVO.class));
        return success(resp);
    }

    @GetMapping("/checkpoint")
    @Operation(summary = "获取编排 Checkpoint 快照")
    @PreAuthorize("@ss.hasPermission('legal:orchestration:query') or @ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalOrchestrationCheckpointRespVO> getCheckpoint(
            @Parameter(description = "AI 对话编号", required = true) @RequestParam("conversationId") Long conversationId) {
        LegalOrchestrationSessionDO session = resolveOwnedSession(conversationId);
        if (session == null) {
            return success(null);
        }
        return checkpointService.loadCheckpoint(session.getId())
                .map(checkpoint -> success(BeanUtils.toBean(checkpoint, LegalOrchestrationCheckpointRespVO.class)))
                .orElse(success(null));
    }

    @PostMapping("/resume")
    @Operation(summary = "从 Checkpoint 恢复编排状态")
    @PreAuthorize("@ss.hasPermission('legal:orchestration:query') or @ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalOrchestrationCheckpointRespVO> resumeFromCheckpoint(
            @Parameter(description = "AI 对话编号", required = true) @RequestParam("conversationId") Long conversationId) {
        LegalOrchestrationSessionDO session = resolveOwnedSession(conversationId);
        if (session == null) {
            return success(null);
        }
        LegalOrchestrationCheckpointService.LegalOrchestrationCheckpoint checkpoint =
                checkpointService.resumeSession(session);
        return success(BeanUtils.toBean(checkpoint, LegalOrchestrationCheckpointRespVO.class));
    }

    private LegalOrchestrationSessionDO resolveOwnedSession(Long conversationId) {
        LegalOrchestrationSessionDO session = sessionService.getByConversationId(conversationId);
        if (session == null) {
            return null;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (!userId.equals(session.getUserId())) {
            return null;
        }
        return session;
    }

    private void enrichPreviewSummary(LegalOrchestrationSessionRespVO resp, Long sessionId) {
        LegalOrchestrationAuditPreviewSnapshotBO snapshot = previewAuditService.getSnapshot(sessionId);
        if (snapshot.getFiles() == null || snapshot.getFiles().isEmpty()) {
            return;
        }
        int opinionCount = 0;
        int highRiskCount = 0;
        for (LegalOrchestrationAuditPreviewSnapshotBO.FilePreview file : snapshot.getFiles()) {
            opinionCount += file.getOpinionCount() != null ? file.getOpinionCount() : 0;
            highRiskCount += file.getHighRiskCount() != null ? file.getHighRiskCount() : 0;
        }
        resp.setPreviewOpinionCount(opinionCount);
        resp.setPreviewHighRiskCount(highRiskCount);
    }

}
