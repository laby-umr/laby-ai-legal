package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.bpm.api.task.BpmProcessInstanceApi;
import com.laby.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.laby.module.bpm.enums.task.BpmTaskStatusEnum;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractOpinionCompleteReqVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.LegalContractConstants;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalContractTaskKeyEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_OPINION_FEEDBACK_REQUIRED;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_OPINION_NOT_EDITABLE;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_OPINION_PENDING_NOT_DISPOSED;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_PROCESS_START_FAILED;

/**
 * 合同 BPM 流程：人工阶段启动、意见复核完成与流程变量同步。
 */
@Slf4j
@Service
public class LegalContractBpmService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private LegalAiAuditService aiAuditService;
    @Resource
    private LegalAiAuditProgressService auditProgressService;
    @Resource
    private LegalContractVersionService contractVersionService;
    @Resource
    private LegalContractExportService exportService;
    @Resource
    private LegalContractQueryService queryService;

    /**
     * 解析与首轮 AI 已在 {@link LegalContractPipelineService} 完成，此处仅启动人工审批流程。
     */
    public void startBpmHumanPhase(Long userId, LegalContractCreateReqVO createReqVO, LegalContractDO contract) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("contractId", contract.getId());
        variables.put("auditRound", 1);
        variables.put("needSecondRound", false);
        variables.put("partyRole", contract.getPartyRole());
        variables.put("auditLevel", contract.getAuditLevel());
        variables.put("contractTypeId", contract.getContractTypeId());
        variables.put("modelId", contract.getModelId());
        variables.put("riskHighCount", 0);

        try {
            String processInstanceId = processInstanceApi.createProcessInstance(userId,
                    new BpmProcessInstanceCreateReqDTO()
                            .setProcessDefinitionKey(LegalContractConstants.PROCESS_KEY)
                            .setBusinessKey(String.valueOf(contract.getId()))
                            .setVariables(variables)
                            .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
            contractMapper.updateById(new LegalContractDO()
                    .setId(contract.getId())
                    .setProcessInstanceId(processInstanceId)
                    .setStatus(LegalContractStatusEnum.OPINION_REVIEW.getStatus())
                    .setCurrentTaskKey(LegalContractTaskKeyEnum.OPINION_REVIEW.getKey()));
        } catch (Exception ex) {
            if (isProcessDefinitionUnavailable(ex)) {
                log.warn("[startBpmHumanPhase][contractId={}] 流程未部署，保持业务态在意见处置（无流程实例）",
                        contract.getId());
                contractMapper.updateById(new LegalContractDO()
                        .setId(contract.getId())
                        .setStatus(LegalContractStatusEnum.OPINION_REVIEW.getStatus())
                        .setCurrentTaskKey(LegalContractTaskKeyEnum.OPINION_REVIEW.getKey()));
                return;
            }
            log.error("[startBpmHumanPhase][contractId={}] 流程发起失败", contract.getId(), ex);
            throw exception(CONTRACT_PROCESS_START_FAILED);
        }
    }

    public void completeOpinionReview(Long userId, LegalContractOpinionCompleteReqVO reqVO) {
        LegalContractDO contract = queryService.validateContractExists(reqVO.getContractId());
        if (!LegalContractPermissionHelper.canCompleteOpinionReview(contract)) {
            throw exception(CONTRACT_OPINION_NOT_EDITABLE);
        }
        if (isRound2OpinionReview(contract)) {
            long pending = opinionMapper.selectPendingCount(contract.getId());
            if (pending > 0) {
                throw exception(CONTRACT_OPINION_PENDING_NOT_DISPOSED);
            }
            finalizeOpinionRound(contract, contract.getAuditRound() == null ? 2 : contract.getAuditRound());
            return;
        }
        if (Boolean.TRUE.equals(reqVO.getNeedSecondRound())) {
            if (StrUtil.isBlank(reqVO.getFeedbackSummary())
                    || reqVO.getFeedbackSummary().length() < LegalContractConstants.FEEDBACK_SUMMARY_MIN_LENGTH) {
                throw exception(CONTRACT_OPINION_FEEDBACK_REQUIRED);
            }
        }
        int highCount = (int) opinionMapper.selectHighRiskPendingCount(contract.getId());
        boolean suggestSecond = highCount > 0;
        boolean needSecond = Boolean.TRUE.equals(reqVO.getNeedSecondRound());

        contractMapper.updateById(new LegalContractDO()
                .setId(contract.getId())
                .setNeedSecondRound(needSecond)
                .setFeedbackSummary(reqVO.getFeedbackSummary())
                .setRiskHighCount(highCount));

        if (needSecond) {
            contractVersionService.prepareSecondAuditRound(contract.getId());
            contractMapper.updateById(new LegalContractDO()
                    .setId(contract.getId())
                    .setStatus(LegalContractStatusEnum.AI_REAUDITING.getStatus())
                    .setCurrentTaskKey(LegalContractTaskKeyEnum.AI_ROUND2.getKey())
                    .setAuditRound(2));
            auditProgressService.clear(contract.getId());
            aiAuditService.auditAsync(contract.getId(), 2);
        } else {
            finalizeOpinionRound(contract, 1);
            if (suggestSecond && !needSecond) {
                log.info("[completeOpinionReview][contractId={}] 存在未处置高风险意见，未申请二轮", contract.getId());
            }
        }

        syncOpinionReviewProcessVariables(contract, needSecond, highCount);
    }

    public void updateContractStatus(Long id, Integer status) {
        queryService.validateContractExists(id);
        contractMapper.updateById(new LegalContractDO().setId(id).setStatus(status));
    }

    public void updateBpmStatus(Long id, Integer bpmStatus) {
        queryService.validateContractExists(id);
        contractMapper.updateById(new LegalContractDO().setId(id).setBpmStatus(bpmStatus));
        if (BpmTaskStatusEnum.APPROVE.getStatus().equals(bpmStatus)) {
            updateContractStatus(id, LegalContractStatusEnum.ARCHIVED.getStatus());
        } else if (BpmTaskStatusEnum.REJECT.getStatus().equals(bpmStatus)) {
            updateContractStatus(id, LegalContractStatusEnum.REJECTED.getStatus());
        } else if (BpmTaskStatusEnum.CANCEL.getStatus().equals(bpmStatus)) {
            updateContractStatus(id, LegalContractStatusEnum.CANCELLED.getStatus());
        }
    }

    private void finalizeOpinionRound(LegalContractDO contract, int auditRound) {
        int highCount = (int) opinionMapper.selectHighRiskPendingCount(contract.getId());
        contractMapper.updateById(new LegalContractDO()
                .setId(contract.getId())
                .setRiskHighCount(highCount));
        try {
            Long zipFileId = exportService.exportArchivePackage(contract.getId());
            log.info("[finalizeOpinionRound][contractId={} round={}] 已生成归档发布包 fileId={}",
                    contract.getId(), auditRound, zipFileId);
        } catch (Exception ex) {
            log.warn("[finalizeOpinionRound][contractId={} round={}] 归档发布包生成失败", contract.getId(), auditRound, ex);
        }
    }

    private static boolean isRound2OpinionReview(LegalContractDO contract) {
        if (LegalContractTaskKeyEnum.REVIEW_ROUND2.getKey().equals(contract.getCurrentTaskKey())) {
            return true;
        }
        return StrUtil.isBlank(contract.getProcessInstanceId())
                && contract.getAuditRound() != null && contract.getAuditRound() > 1;
    }

    private void syncOpinionReviewProcessVariables(LegalContractDO contract, boolean needSecond, int highCount) {
        if (StrUtil.isBlank(contract.getProcessInstanceId())) {
            return;
        }
        Map<String, Object> processVars = new HashMap<>();
        processVars.put("needSecondRound", needSecond);
        processVars.put("riskHighCount", highCount);
        try {
            invokeUpdateProcessInstanceVariables(contract.getProcessInstanceId(), processVars);
        } catch (Exception ex) {
            log.warn("[syncOpinionReviewProcessVariables][contractId={}] 同步流程变量失败: {}",
                    contract.getId(), ex.getMessage());
        }
    }

    private static boolean isProcessDefinitionUnavailable(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && (message.contains("流程定义不存在")
                    || message.contains("no processes deployed")
                    || message.contains("PROCESS_DEFINITION_NOT_EXISTS"))) {
                return true;
            }
        }
        return false;
    }

    private void invokeUpdateProcessInstanceVariables(String processInstanceId, Map<String, Object> variables)
            throws Exception {
        Method method = processInstanceApi.getClass().getMethod(
                "updateProcessInstanceVariables", String.class, Map.class);
        method.invoke(processInstanceApi, processInstanceId, variables);
    }

}
