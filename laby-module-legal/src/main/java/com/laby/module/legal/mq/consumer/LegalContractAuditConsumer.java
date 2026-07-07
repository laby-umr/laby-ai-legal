package com.laby.module.legal.mq.consumer;

import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.legal.mq.message.LegalContractAuditMessage;
import com.laby.module.legal.service.contract.LegalAiAuditService;
import com.laby.module.legal.service.contract.LegalAuditConcurrencyGuard;
import com.laby.framework.mq.redis.core.stream.AbstractRedisStreamMessageListener;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消费合同 AI 审核任务（集群消费 + 租户并发上限）。
 */
@Slf4j
@Component
public class LegalContractAuditConsumer extends AbstractRedisStreamMessageListener<LegalContractAuditMessage> {

    @Resource
    private LegalAiAuditService aiAuditService;
    @Resource
    private LegalAuditConcurrencyGuard auditConcurrencyGuard;

    @Override
    public void onMessage(LegalContractAuditMessage message) {
        Long tenantId = message.getTenantId();
        if (!auditConcurrencyGuard.tryAcquire(tenantId)) {
            throw new IllegalStateException("租户审核并发已满，稍后重试 contractId=" + message.getContractId());
        }
        try {
            TenantUtils.execute(tenantId, () ->
                    aiAuditService.executeAudit(message.getContractId(), message.getAuditRound(), false));
        } finally {
            auditConcurrencyGuard.release(tenantId);
        }
    }

}
