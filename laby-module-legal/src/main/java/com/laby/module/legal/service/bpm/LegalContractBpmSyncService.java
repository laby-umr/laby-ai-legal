package com.laby.module.legal.service.bpm;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalContractTaskKeyEnum;
import com.laby.module.legal.service.contract.LegalContractExportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 将 Flowable 用户任务节点与合同业务状态、currentTaskKey 对齐
 */
@Slf4j
@Service
public class LegalContractBpmSyncService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalContractExportService exportService;
    @Resource
    private TaskService taskService;
    @Resource
    private RuntimeService runtimeService;

    /**
     * 从 Flowable 运行时刷新合同状态（修复异步任务导致 DB 与流程不一致）
     */
    public void refreshFromProcessInstance(Long contractId, String processInstanceId) {
        if (contractId == null || StrUtil.isBlank(processInstanceId)) {
            return;
        }
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract != null && isAiAuditing(contract.getStatus())) {
            // AI 审核长事务持有行锁，此阶段跳过 BPM 回写，避免 Lock wait timeout
            return;
        }
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .orderByTaskCreateTime().desc()
                .list();
        if (!tasks.isEmpty()) {
            onUserTaskCreate(contractId, tasks.get(0).getTaskDefinitionKey());
            return;
        }
        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (Execution execution : executions) {
            String activityId = execution.getActivityId();
            LegalContractStatusEnum status = mapServiceActivity(activityId);
            if (status != null) {
                contractMapper.updateById(new LegalContractDO()
                        .setId(contractId)
                        .setCurrentTaskKey(activityId)
                        .setStatus(status.getStatus()));
                return;
            }
        }
    }

    public void onUserTaskCreate(Long contractId, String taskDefinitionKey) {
        LegalContractStatusEnum status = mapTaskToStatus(taskDefinitionKey);
        if (status == null) {
            return;
        }
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setCurrentTaskKey(taskDefinitionKey)
                .setStatus(status.getStatus()));
        log.info("[onUserTaskCreate][contractId={}] task={} status={}", contractId, taskDefinitionKey, status.getName());
    }

    public void onUserTaskComplete(Long contractId, String taskDefinitionKey) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            return;
        }
        LegalContractTaskKeyEnum taskKey = LegalContractTaskKeyEnum.of(taskDefinitionKey);
        if (taskKey == null) {
            return;
        }
        switch (taskKey) {
            case OPINION_REVIEW -> afterOpinionReviewComplete(contract);
            case REVIEW_ROUND2 -> afterReviewRound2Complete(contract);
            case DIRECTOR_REVIEW -> contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setCurrentTaskKey(LegalContractTaskKeyEnum.FINALIZE.getKey())
                    .setStatus(LegalContractStatusEnum.FINALIZING.getStatus()));
            default -> {
            }
        }
    }

    private void afterOpinionReviewComplete(LegalContractDO contract) {
        Long contractId = contract.getId();
        if (Boolean.TRUE.equals(contract.getNeedSecondRound())) {
            contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setCurrentTaskKey(LegalContractTaskKeyEnum.AI_ROUND2.getKey())
                    .setStatus(LegalContractStatusEnum.AI_REAUDITING.getStatus()));
            return;
        }
        ensureAdoptedVersionsForRound(contract, 1);
        routeAfterOpinionDisposition(contract);
    }

    private void afterReviewRound2Complete(LegalContractDO contract) {
        int auditRound = contract.getAuditRound() == null ? 2 : contract.getAuditRound();
        ensureAdoptedVersionsForRound(contract, auditRound);
        routeAfterOpinionDisposition(contract);
    }

    private void ensureAdoptedVersionsForRound(LegalContractDO contract, int auditRound) {
        int highCount = (int) opinionMapper.selectHighRiskPendingCount(contract.getId());
        contractMapper.updateById(new LegalContractDO()
                .setId(contract.getId())
                .setRiskHighCount(highCount));
        try {
            exportService.exportArchivePackage(contract.getId());
            log.info("[ensureAdoptedVersionsForRound][contractId={} round={}] BPM 兜底生成归档发布包",
                    contract.getId(), auditRound);
        } catch (Exception ex) {
            log.warn("[ensureAdoptedVersionsForRound][contractId={} round={}] 归档发布包失败",
                    contract.getId(), auditRound, ex);
        }
    }

    private void routeAfterOpinionDisposition(LegalContractDO contract) {
        Long contractId = contract.getId();
        int high = contract.getRiskHighCount() != null ? contract.getRiskHighCount() : 0;
        if (high > 0) {
            contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setCurrentTaskKey(LegalContractTaskKeyEnum.DIRECTOR_GATEWAY.getKey())
                    .setStatus(LegalContractStatusEnum.DIRECTOR_REVIEW.getStatus()));
        } else {
            contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setCurrentTaskKey(LegalContractTaskKeyEnum.FINALIZE.getKey())
                    .setStatus(LegalContractStatusEnum.FINALIZING.getStatus()));
        }
    }

    private static LegalContractStatusEnum mapTaskToStatus(String taskDefinitionKey) {
        LegalContractTaskKeyEnum taskKey = LegalContractTaskKeyEnum.of(taskDefinitionKey);
        return taskKey != null ? taskKey.toUserTaskStatus() : null;
    }

    private static LegalContractStatusEnum mapServiceActivity(String activityId) {
        LegalContractTaskKeyEnum taskKey = LegalContractTaskKeyEnum.of(activityId);
        return taskKey != null ? taskKey.toServiceActivityStatus() : null;
    }

    private static boolean isAiAuditing(Integer status) {
        return LegalContractStatusEnum.AI_AUDITING.getStatus().equals(status)
                || LegalContractStatusEnum.AI_REAUDITING.getStatus().equals(status);
    }

}
