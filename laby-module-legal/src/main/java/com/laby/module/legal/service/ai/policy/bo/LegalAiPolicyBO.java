package com.laby.module.legal.service.ai.policy.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 法务 AI 统一策略快照（编排 / Pipeline / 合同 QA 共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAiPolicyBO {

    /** 大模型编号（必填） */
    private Long modelId;

    /** 我方立场，见 {@link com.laby.module.legal.enums.contract.LegalPartyRoleEnum} */
    private String partyRole;

    /** 审核强度，见 {@link com.laby.module.legal.enums.contract.LegalAuditLevelEnum} */
    private String auditLevel;

    /** 首轮 AI 审核角色 ai_chat_role.id */
    private Long auditRoleId;

    /** 二轮审核角色（可选） */
    private Long reauditRoleId;

    /** 追溯：AI 对话编号 */
    private Long conversationId;

    /** 策略版本 */
    private String policyVersion;

    /** 冻结的技能包快照 JSON（预览/提案时写入，创建合同时复用） */
    private String skillPackSnapshotJson;

    /** 快照对应的合同类型编号 */
    private Long skillPackSnapshotContractTypeId;

}
