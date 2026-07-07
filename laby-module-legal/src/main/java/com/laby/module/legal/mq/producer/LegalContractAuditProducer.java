package com.laby.module.legal.mq.producer;

import com.laby.framework.mq.redis.core.RedisMQTemplate;
import com.laby.module.legal.mq.message.LegalContractAuditMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 合同 AI 审核任务投递。
 */
@Slf4j
@Component
public class LegalContractAuditProducer {

    @Resource
    private RedisMQTemplate redisMQTemplate;

    public void send(Long contractId, int auditRound, Long tenantId) {
        LegalContractAuditMessage message = new LegalContractAuditMessage();
        message.setContractId(contractId);
        message.setAuditRound(auditRound);
        message.setTenantId(tenantId);
        redisMQTemplate.send(message);
        log.info("[send][contractId={} auditRound={} tenantId={}] 审核任务已入队", contractId, auditRound, tenantId);
    }

}
