package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalContractTaskKeyEnum;
import com.laby.module.legal.enums.contract.LegalParseStatusEnum;

import java.util.Set;

/**
 * 合同业务操作权限（与 BPM 节点对齐）
 */
public final class LegalContractPermissionHelper {

    private static final Set<Integer> TERMINAL_STATUSES = Set.of(
            LegalContractStatusEnum.ARCHIVED.getStatus(),
            LegalContractStatusEnum.REJECTED.getStatus(),
            LegalContractStatusEnum.CANCELLED.getStatus()
    );

    private LegalContractPermissionHelper() {
    }

    public static boolean isTerminal(Integer status) {
        return status != null && TERMINAL_STATUSES.contains(status);
    }

    /** 可保存处置结果、勾选二轮（首轮意见节点）或完成二轮复核 */
    public static boolean canCompleteOpinionReview(LegalContractDO contract) {
        if (contract == null || isTerminal(contract.getStatus()) || isAiAuditing(contract.getStatus())) {
            return false;
        }
        if (StrUtil.isBlank(contract.getProcessInstanceId())) {
            return LegalContractStatusEnum.OPINION_REVIEW.getStatus().equals(contract.getStatus());
        }
        String taskKey = contract.getCurrentTaskKey();
        return LegalContractTaskKeyEnum.OPINION_REVIEW.getKey().equals(taskKey)
                || LegalContractTaskKeyEnum.REVIEW_ROUND2.getKey().equals(taskKey);
    }

    /** 可保存处置结果（仅首轮意见节点，用于二轮申请校验） */
    public static boolean canDisposeOpinion(LegalContractDO contract) {
        if (contract == null || isTerminal(contract.getStatus()) || isAiAuditing(contract.getStatus())) {
            return false;
        }
        if (StrUtil.isBlank(contract.getProcessInstanceId())) {
            return LegalContractStatusEnum.OPINION_REVIEW.getStatus().equals(contract.getStatus())
                    && (contract.getAuditRound() == null || contract.getAuditRound() <= 1);
        }
        return LegalContractTaskKeyEnum.OPINION_REVIEW.getKey().equals(contract.getCurrentTaskKey());
    }

    /** 可采纳/忽略/补充意见 */
    public static boolean canManageOpinions(LegalContractDO contract) {
        return canManageOpinions(contract, 0L);
    }

    /**
     * 可采纳/忽略/补充意见（含流程已越过复核节点但仍有待处置意见的兜底）
     */
    public static boolean canManageOpinions(LegalContractDO contract, long pendingOpinionCount) {
        if (contract == null || isTerminal(contract.getStatus()) || isAiAuditing(contract.getStatus())) {
            return false;
        }
        if (StrUtil.isBlank(contract.getProcessInstanceId())) {
            return LegalContractStatusEnum.OPINION_REVIEW.getStatus().equals(contract.getStatus());
        }
        if (LegalContractTaskKeyEnum.isOpinionTaskKey(contract.getCurrentTaskKey())) {
            return true;
        }
        return pendingOpinionCount > 0 && isLateStageOpinionRecoverable(contract);
    }

    /** 列表是否显示「审核/办理」入口（有流程实例且未结束即可进入流程详情/待办） */
    public static boolean canOpenReviewWorkbench(LegalContractDO contract) {
        if (contract == null || isTerminal(contract.getStatus())) {
            return false;
        }
        if (LegalContractStatusEnum.FAILED.getStatus().equals(contract.getStatus())) {
            return false;
        }
        if (LegalContractStatusEnum.PARSING.getStatus().equals(contract.getStatus())
                || LegalContractStatusEnum.AI_AUDITING.getStatus().equals(contract.getStatus())) {
            return false;
        }
        if (canStartFirstAudit(contract)) {
            return true;
        }
        if (StrUtil.isNotBlank(contract.getProcessInstanceId())) {
            return true;
        }
        return LegalContractStatusEnum.OPINION_REVIEW.getStatus().equals(contract.getStatus());
    }

    /** 已解析、尚未首轮 AI 审核（AI 对话创建落库后的待审核态） */
    public static boolean canStartFirstAudit(LegalContractDO contract) {
        if (contract == null || isTerminal(contract.getStatus())) {
            return false;
        }
        if (!LegalContractStatusEnum.DRAFT.getStatus().equals(contract.getStatus())) {
            return false;
        }
        if (!LegalParseStatusEnum.SUCCESS.getStatus().equals(contract.getParseStatus())) {
            return false;
        }
        return StrUtil.isBlank(contract.getProcessInstanceId());
    }

    public static boolean canRetryPipeline(LegalContractDO contract) {
        return contract != null
                && LegalContractStatusEnum.FAILED.getStatus().equals(contract.getStatus());
    }

    public static boolean canApplySecondRound(LegalContractDO contract) {
        return canDisposeOpinion(contract);
    }

    private static boolean isAiAuditing(Integer status) {
        return LegalContractStatusEnum.AI_AUDITING.getStatus().equals(status)
                || LegalContractStatusEnum.AI_REAUDITING.getStatus().equals(status);
    }

    private static boolean isLateStageOpinionRecoverable(LegalContractDO contract) {
        String taskKey = contract.getCurrentTaskKey();
        if (LegalContractTaskKeyEnum.FINALIZE.getKey().equals(taskKey)
                || LegalContractTaskKeyEnum.DIRECTOR_REVIEW.getKey().equals(taskKey)) {
            return true;
        }
        return LegalContractStatusEnum.FINALIZING.getStatus().equals(contract.getStatus())
                || LegalContractStatusEnum.DIRECTOR_REVIEW.getStatus().equals(contract.getStatus());
    }

}
