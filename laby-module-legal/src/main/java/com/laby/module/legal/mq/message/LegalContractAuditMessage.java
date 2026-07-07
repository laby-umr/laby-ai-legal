package com.laby.module.legal.mq.message;

import com.laby.framework.mq.redis.core.stream.AbstractRedisStreamMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 合同 AI 审核任务（Redis Stream）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalContractAuditMessage extends AbstractRedisStreamMessage {

    private Long contractId;

    private Integer auditRound;

    private Long tenantId;

}
