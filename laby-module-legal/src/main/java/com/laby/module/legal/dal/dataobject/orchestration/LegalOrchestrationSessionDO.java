package com.laby.module.legal.dal.dataobject.orchestration;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 法务 AI 编排会话 DO
 */
@TableName("legal_orchestration_session")
@KeySequence("legal_orchestration_session_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalOrchestrationSessionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long conversationId;

    private Long userId;

    /** 编排阶段，见 LegalOrchestrationPhaseEnum */
    private String phase;

    private Long modelId;

    /** 我方立场，见 LegalPartyRoleEnum */
    private String partyRole;

    /** 审核强度，见 LegalAuditLevelEnum */
    private String auditLevel;

    /** 首轮 AI 审核角色 ai_chat_role.id */
    private Long auditRoleId;

    /** LegalAiPolicyBO JSON 快照 */
    private String policyJson;

    /** 预览审核结果 JSON 快照 */
    private String previewOpinionJson;

    /** 编排阶段 Checkpoint JSON */
    private String checkpointJson;

    private String remark;

}
