package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummarySupport;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractChatMessageMapper;
import com.laby.module.legal.framework.agentscope.chat.LegalContractCompactionSummaryContext;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 将 Harness Compaction 摘要写入 {@code legal_contract_chat_message}（type=summary）。
 */
@Slf4j
@Service
public class LegalContractCompactionSummaryService {

    public static final String MESSAGE_TYPE_SUMMARY = "summary";

    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private LegalContractChatMessageMapper chatMessageMapper;

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
            persistSummary(context, summary);
        }
    }

    private void persistSummary(LegalContractCompactionSummaryContext context, String summary) {
        if (chatMessageMapper.existsSummaryContent(context.getContractId(), context.getSessionId(), summary)) {
            return;
        }
        LegalContractChatMessageDO message = LegalContractChatMessageDO.builder()
                .contractId(context.getContractId())
                .userId(context.getUserId())
                .type(MESSAGE_TYPE_SUMMARY)
                .content(summary)
                .sessionId(context.getSessionId())
                .agentMode(Boolean.TRUE)
                .build();
        chatMessageMapper.insert(message);
        log.info("[persistSummary][contractId={}] compaction summary saved, id={}",
                context.getContractId(), message.getId());
    }

}
