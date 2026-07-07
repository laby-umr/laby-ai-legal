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

/**
 * 法务 Agent 步骤日志 DO
 */
@TableName("legal_agent_step_log")
@KeySequence("legal_agent_step_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAgentStepLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 合同编号 */
    private Long contractId;

    /** 发起问答的用户编号 */
    private Long userId;

    /** 前端一次问答 session */
    private String sessionId;

    /** 步骤序号，从 1 递增 */
    private Integer stepIndex;

    /** LLM / TOOL / ERROR */
    private String stepType;

    /** Tool Bean 名 */
    private String toolName;

    /** 脱敏截断后的入参 JSON */
    private String toolInputJson;

    /** 出参摘要 */
    private String toolOutputSummary;

    /** 耗时毫秒 */
    private Integer latencyMs;

}
