package com.laby.module.legal.service.orchestration;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.service.ai.kernel.LegalAuditKernel;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelResult;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditPreviewCommand;
import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationAuditPreviewItemBO;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationAuditPreviewSnapshotBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_PREVIEW_FILE_MISSING;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_TYPE_NOT_RESOLVED;

/**
 * 编排预览审核：调用 AuditKernel 并写入 session.preview_opinion_json
 */
@Service
public class LegalOrchestrationPreviewAuditService {

    @Resource
    private LegalOrchestrationSessionService sessionService;
    @Resource
    private LegalOrchestrationSessionMapper sessionMapper;
    @Resource
    private LegalAiPolicyResolver policyResolver;
    @Resource
    private LegalAuditKernel auditKernel;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;

    @Transactional(rollbackFor = Exception.class)
    public LegalOrchestrationAuditPreviewSnapshotBO preview(Long sessionId, Long fileItemId,
                                                            Long modelIdHint) {
        LegalOrchestrationSessionDO session = sessionService.validateSessionExists(sessionId);
        LegalAiPolicyBO policy = policyResolver.resolveForSession(session, modelIdHint, null, null);
        sessionService.syncPolicy(sessionId, policy);

        List<LegalOrchestrationFileItemDO> targets = resolveTargetFiles(sessionId, fileItemId);
        if (CollUtil.isNotEmpty(targets)) {
            skillPackSnapshotService.freezeSnapshotOnPolicy(policy, resolveTypeId(targets.get(0)));
            sessionService.syncPolicy(sessionId, policy);
        }
        LegalOrchestrationAuditPreviewSnapshotBO snapshot = loadSnapshot(session);
        snapshot.setPolicyVersion(policy.getPolicyVersion());
        snapshot.setModelId(policy.getModelId());
        snapshot.setUpdatedAt(LocalDateTime.now());
        if (snapshot.getFiles() == null) {
            snapshot.setFiles(new ArrayList<>());
        }

        for (LegalOrchestrationFileItemDO file : targets) {
            if (file.getInfraFileId() == null) {
                throw exception(ORCHESTRATION_PREVIEW_FILE_MISSING);
            }
            Long typeId = resolveTypeId(file);
            LegalAuditKernelResult result = auditKernel.runPreview(LegalAuditPreviewCommand.builder()
                    .policy(policy)
                    .sessionId(sessionId)
                    .fileItemId(file.getId())
                    .contractTypeId(typeId)
                    .infraFileId(file.getInfraFileId())
                    .fileName(file.getFileName())
                    .build());
            LegalOrchestrationAuditPreviewSnapshotBO.FilePreview filePreview = toFilePreview(file, result.getItems());
            replaceFilePreview(snapshot, filePreview);
        }

        sessionMapper.updateById(new LegalOrchestrationSessionDO()
                .setId(sessionId)
                .setPreviewOpinionJson(JsonUtils.toJsonString(snapshot)));
        return snapshot;
    }

    public LegalOrchestrationAuditPreviewSnapshotBO getSnapshot(Long sessionId) {
        LegalOrchestrationSessionDO session = sessionService.validateSessionExists(sessionId);
        return loadSnapshot(session);
    }

    private List<LegalOrchestrationFileItemDO> resolveTargetFiles(Long sessionId, Long fileItemId) {
        List<LegalOrchestrationFileItemDO> files = sessionService.listFileItems(sessionId);
        if (fileItemId != null) {
            LegalOrchestrationFileItemDO file = files.stream()
                    .filter(item -> Objects.equals(item.getId(), fileItemId))
                    .findFirst()
                    .orElseThrow(() -> exception(ORCHESTRATION_TYPE_NOT_RESOLVED));
            requireConfirmedType(file);
            return List.of(file);
        }
        List<LegalOrchestrationFileItemDO> classified = files.stream()
                .filter(file -> file.getConfirmedTypeId() != null)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(classified)) {
            throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);
        }
        return classified;
    }

    private static void requireConfirmedType(LegalOrchestrationFileItemDO file) {
        if (file.getConfirmedTypeId() == null) {
            throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);
        }
    }

    private static Long resolveTypeId(LegalOrchestrationFileItemDO file) {
        requireConfirmedType(file);
        return file.getConfirmedTypeId();
    }

    private static LegalOrchestrationAuditPreviewSnapshotBO loadSnapshot(LegalOrchestrationSessionDO session) {
        if (session == null || StrUtil.isBlank(session.getPreviewOpinionJson())) {
            LegalOrchestrationAuditPreviewSnapshotBO empty = new LegalOrchestrationAuditPreviewSnapshotBO();
            empty.setFiles(new ArrayList<>());
            return empty;
        }
        LegalOrchestrationAuditPreviewSnapshotBO snapshot = JsonUtils.parseObject(
                session.getPreviewOpinionJson(), LegalOrchestrationAuditPreviewSnapshotBO.class);
        if (snapshot == null) {
            snapshot = new LegalOrchestrationAuditPreviewSnapshotBO();
        }
        if (snapshot.getFiles() == null) {
            snapshot.setFiles(new ArrayList<>());
        }
        return snapshot;
    }

    private static void replaceFilePreview(LegalOrchestrationAuditPreviewSnapshotBO snapshot,
                                           LegalOrchestrationAuditPreviewSnapshotBO.FilePreview filePreview) {
        snapshot.getFiles().removeIf(file -> Objects.equals(file.getFileItemId(), filePreview.getFileItemId()));
        snapshot.getFiles().add(filePreview);
    }

    private static LegalOrchestrationAuditPreviewSnapshotBO.FilePreview toFilePreview(
            LegalOrchestrationFileItemDO file, List<LegalAiAuditOpinionItemBO> items) {
        LegalOrchestrationAuditPreviewSnapshotBO.FilePreview preview =
                new LegalOrchestrationAuditPreviewSnapshotBO.FilePreview();
        preview.setFileItemId(file.getId());
        preview.setFileName(file.getFileName());
        List<LegalOrchestrationAuditPreviewItemBO> opinions = new ArrayList<>();
        int highCount = 0;
        if (CollUtil.isNotEmpty(items)) {
            for (LegalAiAuditOpinionItemBO item : items) {
                LegalOrchestrationAuditPreviewItemBO row = new LegalOrchestrationAuditPreviewItemBO();
                row.setFileItemId(file.getId());
                row.setFileName(file.getFileName());
                row.setTitle(StrUtil.blankToDefault(item.getTitle(), "审核意见"));
                row.setRiskLevel(LegalRiskLevelEnum.normalize(item.getRiskLevel()).getCode());
                row.setClauseType(item.getClauseType());
                row.setParagraphId(item.getParagraphId());
                row.setSourceType(item.getSourceType());
                row.setContent(item.getContent());
                row.setSuggestion(item.getSuggestion());
                opinions.add(row);
                if (LegalRiskLevelEnum.HIGH.getCode().equals(row.getRiskLevel())) {
                    highCount++;
                }
            }
        }
        preview.setOpinions(opinions);
        preview.setOpinionCount(opinions.size());
        preview.setHighRiskCount(highCount);
        return preview;
    }

}
