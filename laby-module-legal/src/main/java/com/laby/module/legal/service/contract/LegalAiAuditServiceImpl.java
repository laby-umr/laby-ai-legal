package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.module.legal.controller.admin.contract.vo.LegalAiAuditProgressRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalContractTaskKeyEnum;
import com.laby.module.legal.framework.config.LegalPlaybookProperties;
import com.laby.module.legal.mq.producer.LegalContractAuditProducer;
import com.laby.module.legal.service.ai.LegalAiModelResolver;
import com.laby.module.legal.service.bpm.LegalContractBpmAuditSignalService;
import com.laby.module.legal.service.ai.kernel.LegalAuditKernel;
import com.laby.module.legal.service.ai.kernel.LegalAuditPreviewReuseService;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelCommand;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelResult;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditPreviewMergeResult;
import com.laby.module.legal.service.contract.bo.LegalContractAuditPersistCommand;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import com.laby.module.legal.service.trace.LegalAiTraceRecorder;
import com.laby.module.legal.service.trace.LegalAiTraceRecorder.TraceSession;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_AI_AUDIT_FAILED;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_PARAGRAPH_EMPTY;

/**
 * 法务合同 AI 审核 Service 实现类
 * <p>
 * 负责分批调用大模型生成审核意见、委托持久化并更新合同状态。
 */
@Slf4j
@Service
public class LegalAiAuditServiceImpl implements LegalAiAuditService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalAiModelResolver legalAiModelResolver;
    @Resource
    private LegalContractVersionService contractVersionService;
    @Resource
    private LegalAuditKernel legalAuditKernel;
    @Resource
    private LegalAuditPreviewReuseService previewReuseService;
    @Resource
    private LegalPlaybookProperties playbookProperties;
    @Resource
    private LegalAiTraceRecorder aiTraceRecorder;
    @Resource
    private LegalContractAuditProducer auditProducer;
    @Resource
    private LegalAiAuditProgressService auditProgressService;
    @Resource
    private LegalContractBpmAuditSignalService bpmAuditSignalService;
    @Resource
    private LegalAuditOpinionPersistService opinionPersistService;

    @Override
    public void auditAsync(Long contractId, int auditRound) {
        LegalContractDO contract = TenantUtils.executeIgnore(() -> contractMapper.selectById(contractId));
        if (contract == null) {
            log.warn("[auditAsync][contractId={}] 合同不存在", contractId);
            return;
        }
        auditProducer.send(contractId, auditRound, contract.getTenantId());
    }

    @Override
    public void auditForBpm(Long contractId, int auditRound) {
        enqueueAuditForBpm(contractId, auditRound);
    }

    @Override
    public void waitForAuditRoundComplete(Long contractId, int auditRound) {
        enqueueAuditForBpm(contractId, auditRound);
    }

    @Override
    public void enqueueAuditForBpm(Long contractId, int auditRound) {
        LegalContractDO contract = TenantUtils.executeIgnore(() -> contractMapper.selectById(contractId));
        if (contract == null) {
            log.warn("[enqueueAuditForBpm][contractId={}] 合同不存在", contractId);
            return;
        }
        ensureAuditEnqueued(contractId, auditRound, contract.getTenantId());
    }

    private void ensureAuditEnqueued(Long contractId, int auditRound, Long tenantId) {
        LegalAiAuditProgressRespVO progress = auditProgressService.get(contractId);
        String status = progress.getStatus();
        if ("RUNNING".equals(status) || "COMPLETED".equals(status)) {
            return;
        }
        TenantUtils.execute(tenantId, () -> auditAsync(contractId, auditRound));
    }

    @Override
    public void auditForPipeline(Long contractId, int auditRound) {
        executeAudit(contractId, auditRound, true);
    }

    @Override
    public void executeAudit(Long contractId, int auditRound, boolean failFast) {
        doAudit(contractId, auditRound, failFast);
    }

    private void doAudit(Long contractId, int auditRound, boolean failFast) {
        LegalContractStatusEnum auditing = auditRound > 1
                ? LegalContractStatusEnum.AI_REAUDITING
                : LegalContractStatusEnum.AI_AUDITING;
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setStatus(auditing.getStatus())
                .setAuditRound(auditRound)
                .setCurrentTaskKey(auditRound > 1
                        ? LegalContractTaskKeyEnum.AI_ROUND2.getKey()
                        : LegalContractTaskKeyEnum.AI_ROUND1.getKey()));

        try {
            LegalContractDO contract = contractMapper.selectById(contractId);
            if (contract == null) {
                throw exception(CONTRACT_NOT_EXISTS);
            }
            if (auditRound > 1) {
                contractVersionService.prepareSecondAuditRound(contractId);
                if (opinionPersistService.hasExistingOpinionsForRound(contractId, auditRound)) {
                    log.info("[doAudit][contractId={} round={}] 已有审核意见，跳过重复二轮", contractId, auditRound);
                    auditProgressService.complete(contractId, "二轮审核意见已存在，跳过重复执行");
                    contractMapper.updateById(new LegalContractDO()
                            .setId(contractId)
                            .setStatus(LegalContractStatusEnum.OPINION_REVIEW.getStatus())
                            .setCurrentTaskKey(LegalContractTaskKeyEnum.OPINION_REVIEW.getKey()));
                    notifyBpmAwaitAiRound2(contractId, auditRound);
                    return;
                }
            }
            List<LegalContractParagraphDO> paragraphs = paragraphMapper.selectListByContractId(contractId).stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getSkipAudit()))
                    .toList();
            if (CollUtil.isEmpty(paragraphs)) {
                throw exception(CONTRACT_PARAGRAPH_EMPTY);
            }

            AiModelDO model = legalAiModelResolver.requireChatModel(contract.getModelId());
            AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
            boolean modelFallback = LegalAiModelResolver.isModelFallback(contract.getModelId(), model.getId());

            TraceSession traceSession = aiTraceRecorder.startAudit(
                    contractId, auditRound, model.getId(), platform.getPlatform());
            int totalBatches = (paragraphs.size() + 5 - 1) / 5;
            auditProgressService.start(contractId, auditRound, totalBatches);
            List<LegalAiAuditOpinionItemBO> items;
            int deterministicCount = 0;
            int previewReuseCount = 0;
            int previewDedupeCount = 0;
            try {
                LegalAuditKernelResult kernelResult = legalAuditKernel.runFormal(
                        LegalAuditKernelCommand.builder()
                                .contract(contract)
                                .paragraphs(paragraphs)
                                .auditRound(auditRound)
                                .failFast(failFast)
                                .playbookEnabled(Boolean.TRUE.equals(playbookProperties.getEnabled()))
                                .build());
                items = kernelResult.getItems();
                deterministicCount = kernelResult.getDeterministicCount();

                Optional<List<LegalAiAuditOpinionItemBO>> previewItems =
                        previewReuseService.resolvePreviewItems(contract, auditRound);
                if (previewItems.isPresent()) {
                    LegalAuditPreviewMergeResult mergeResult =
                            previewReuseService.merge(items, previewItems.get());
                    items = mergeResult.getItems();
                    previewReuseCount = mergeResult.getReusedFromPreviewCount();
                    previewDedupeCount = mergeResult.getDedupeCount();
                    log.info("[doAudit][contractId={}] 预览复用：preview={} formal={} dedupe={} reused={}",
                            contractId, mergeResult.getPreviewCount(), mergeResult.getFormalCount(),
                            previewDedupeCount, previewReuseCount);
                }

                auditProgressService.complete(contractId,
                        "AI 审核完成，共 " + CollUtil.size(items) + " 条意见（含 Playbook "
                                + deterministicCount + " 条）");
                aiTraceRecorder.complete(traceSession, CollUtil.size(items), deterministicCount,
                        previewReuseCount, previewDedupeCount, modelFallback);
            } catch (Exception auditEx) {
                auditProgressService.fail(contractId, auditEx.getMessage());
                aiTraceRecorder.fail(traceSession, auditEx.getMessage());
                throw auditEx;
            }

            opinionPersistService.persist(LegalContractAuditPersistCommand.builder()
                    .contractId(contractId)
                    .contract(contract)
                    .auditRound(auditRound)
                    .items(items)
                    .build());

            int highCount = (int) opinionMapper.selectHighRiskPendingCount(contractId);
            contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setStatus(LegalContractStatusEnum.OPINION_REVIEW.getStatus())
                    .setCurrentTaskKey(resolvePostAuditTaskKey(contract, auditRound))
                    .setRiskHighCount(highCount));
            notifyBpmAwaitAiRound2(contractId, auditRound);
        } catch (Exception ex) {
            log.error("[doAudit][contractId={} round={}] AI 审核失败", contractId, auditRound, ex);
            auditProgressService.fail(contractId, ex.getMessage());
            if (failFast) {
                throw exception(CONTRACT_AI_AUDIT_FAILED);
            }
            recoverAfterAuditFailure(contractId, auditRound, ex);
        }
    }

    /**
     * 异步/BPM 容错：仍写入六章报告（含失败说明），避免卡在「AI 审核中」且前端无报告。
     */
    private void recoverAfterAuditFailure(Long contractId, int auditRound, Exception ex) {
        try {
            LegalContractDO contract = contractMapper.selectById(contractId);
            if (contract == null) {
                return;
            }
            opinionPersistService.persist(LegalContractAuditPersistCommand.builder()
                    .contractId(contractId)
                    .contract(contract)
                    .auditRound(auditRound)
                    .items(List.of())
                    .build());
            String note = StrUtil.sub(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 0, 500);
            opinionPersistService.appendFailureNoteToReport(contractId, auditRound, note);
            contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setStatus(LegalContractStatusEnum.OPINION_REVIEW.getStatus())
                    .setCurrentTaskKey(resolvePostAuditTaskKey(contract, auditRound))
                    .setFeedbackSummary(note));
            notifyBpmAwaitAiRound2(contractId, auditRound);
        } catch (Exception recoveryEx) {
            log.error("[recoverAfterAuditFailure][contractId={} round={}]", contractId, auditRound, recoveryEx);
        }
    }

    @Override
    public LegalAuditReportDO rebuildAuditReportIfMissing(Long contractId, int auditRound) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        return opinionPersistService.rebuildAuditReportIfMissing(contractId, auditRound, contract);
    }

    private void notifyBpmAwaitAiRound2(Long contractId, int auditRound) {
        if (auditRound != 2) {
            return;
        }
        bpmAuditSignalService.signalAwaitAiRound2IfSettled(contractId);
    }

    private static String resolvePostAuditTaskKey(LegalContractDO contract, int auditRound) {
        if (auditRound > 1 && StrUtil.isNotBlank(contract.getProcessInstanceId())) {
            return LegalContractTaskKeyEnum.REVIEW_ROUND2.getKey();
        }
        return LegalContractTaskKeyEnum.OPINION_REVIEW.getKey();
    }

}
