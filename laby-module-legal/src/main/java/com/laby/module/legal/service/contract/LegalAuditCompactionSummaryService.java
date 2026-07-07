package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummarySupport;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.legal.enums.memory.LegalContractMemoryTypeEnum;
import com.laby.module.legal.framework.agentscope.chat.LegalContractCompactionSummaryContext;
import com.laby.module.legal.service.memory.LegalContractEpisodicMemoryServiceImpl;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 审核编排 Harness Compaction 摘要写入情节记忆（type=compaction_summary）。
 */
@Slf4j
@Service
public class LegalAuditCompactionSummaryService {

    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private LegalContractEpisodicMemoryServiceImpl episodicMemoryService;

    public boolean isEnabled() {
        return agentScopeProperties.isCompactionSummaryPersist();
    }

    public void persistNewSummaries(HarnessAgent agent, LegalContractCompactionSummaryContext context,
                                    Set<String> existingHashes) {
        if (!isEnabled() || agent == null || context == null || context.getContractId() == null) {
            return;
        }
        List<String> summaries = AiChatCompactionSummarySupport.extractNewSummaries(agent, existingHashes);
        for (String summary : summaries) {
            if (StrUtil.isBlank(summary)) {
                continue;
            }
            episodicMemoryService.saveMemory(
                    context.getContractId(),
                    context.getSessionId(),
                    LegalContractMemoryTypeEnum.COMPACTION_SUMMARY.getType(),
                    summary,
                    null);
            log.info("[persistAuditSummary][contractId={}] compaction summary saved", context.getContractId());
        }
    }

}
