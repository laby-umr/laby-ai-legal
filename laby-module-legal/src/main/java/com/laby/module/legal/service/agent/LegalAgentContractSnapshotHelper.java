package com.laby.module.legal.service.agent;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import lombok.Data;

import java.util.List;

/**
 * 为 Agent 问答注入精简审核快照，避免「已审完合同却被回答审核不存在」。
 */
public final class LegalAgentContractSnapshotHelper {

    private LegalAgentContractSnapshotHelper() {
    }

    @Data
    public static class Snapshot {
        private int totalOpinionCount;
        private int pendingCount;
        private int adoptedCount;
        private int ignoredCount;
        private int highRiskTotalCount;
        private int highRiskPendingCount;
        private int highRiskAdoptedCount;
        private boolean hasAuditOpinions;
    }

    public static Snapshot build(LegalContractDO contract, LegalAuditOpinionMapper opinionMapper) {
        Snapshot snapshot = new Snapshot();
        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractId(contract.getId());
        snapshot.totalOpinionCount = opinions.size();
        snapshot.hasAuditOpinions = !opinions.isEmpty();
        for (LegalAuditOpinionDO opinion : opinions) {
            if (LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                snapshot.pendingCount++;
            } else if (LegalOpinionStatusEnum.ADOPTED.getStatus().equals(opinion.getStatus())) {
                snapshot.adoptedCount++;
            } else if (LegalOpinionStatusEnum.IGNORED.getStatus().equals(opinion.getStatus())) {
                snapshot.ignoredCount++;
            }
            if (LegalRiskLevelEnum.HIGH.getCode().equalsIgnoreCase(opinion.getRiskLevel())) {
                snapshot.highRiskTotalCount++;
                if (LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                    snapshot.highRiskPendingCount++;
                } else if (LegalOpinionStatusEnum.ADOPTED.getStatus().equals(opinion.getStatus())) {
                    snapshot.highRiskAdoptedCount++;
                }
            }
        }
        return snapshot;
    }

    public static String toPromptBlock(LegalContractDO contract, LegalAuditOpinionMapper opinionMapper) {
        Snapshot snapshot = build(contract, opinionMapper);
        String statusName = resolveStatusName(contract.getStatus());
        int auditRound = contract.getAuditRound() != null ? contract.getAuditRound() : 1;
        return """

                【当前会话已绑定合同 — 审核快照（事实来源，勿称「合同审核不存在」）】
                - 合同ID：%d，标题：%s
                - 业务状态：%s（%s），当前审核轮次：%d
                - 审核意见总数：%d 条（待处置 %d / 已采纳 %d / 已忽略 %d）
                - 高风险意见：共 %d 条（待处置 %d / 已采纳 %d）
                - 合同字段 riskHighCount（待处置高风险数）：%s
                说明：totalOpinionCount>0 表示 AI 审核已完成并产生意见；问「有多少高风险」时直接引用上述数字，或再调 legal_get_audit_opinions(riskLevel=HIGH) 核实。
                """.formatted(
                contract.getId(),
                StrUtil.blankToDefault(contract.getTitle(), "-"),
                contract.getStatus(),
                statusName,
                auditRound,
                snapshot.totalOpinionCount,
                snapshot.pendingCount,
                snapshot.adoptedCount,
                snapshot.ignoredCount,
                snapshot.highRiskTotalCount,
                snapshot.highRiskPendingCount,
                snapshot.highRiskAdoptedCount,
                contract.getRiskHighCount() != null ? contract.getRiskHighCount() : 0);
    }

    private static String resolveStatusName(Integer status) {
        if (status == null) {
            return "-";
        }
        for (LegalContractStatusEnum item : LegalContractStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item.getName();
            }
        }
        return String.valueOf(status);
    }

}
