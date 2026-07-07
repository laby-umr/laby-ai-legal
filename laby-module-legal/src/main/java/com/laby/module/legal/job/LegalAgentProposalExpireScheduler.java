package com.laby.module.legal.job;

import com.laby.module.legal.service.agent.LegalAgentProposalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时将已过期的 Agent 提案标记为 EXPIRED
 */
@Slf4j
@Component
public class LegalAgentProposalExpireScheduler {

    @Resource
    private LegalAgentProposalService proposalService;

    @Scheduled(cron = "0 * * * * ?")
    public void expirePendingProposals() {
        int count = proposalService.expirePendingProposals();
        if (count > 0) {
            log.info("[expirePendingProposals] 已过期 {} 条待处理提案", count);
        }
    }

}
