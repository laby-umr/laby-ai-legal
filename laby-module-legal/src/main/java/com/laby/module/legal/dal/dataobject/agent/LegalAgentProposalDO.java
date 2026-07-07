package com.laby.module.legal.dal.dataobject.agent;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 法务 Agent 写操作提案 DO（需用户 Confirm 后执行）
 */
@TableName("legal_agent_proposal")
@KeySequence("legal_agent_proposal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAgentProposalDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 对外提案编号（UUID） */
    private String proposalNo;

    private Long contractId;

    /** AI 对话编号（全局编排） */
    private Long conversationId;

    private Long userId;

    private String sessionId;

    /** ADOPT_OPINION / SKIP_PARAGRAPH */
    private String action;

    /** PENDING / EXECUTED / CANCELLED / EXPIRED */
    private String status;

    /** 展示标题 */
    private String title;

    /** 业务参数 JSON */
    private String payloadJson;

    private LocalDateTime expireTime;

}
