package com.laby.module.ai.service.chat;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.chat.AiChatMessageDO;
import com.laby.module.ai.dal.mysql.chat.AiChatMessageMapper;
import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummaryContext;
import com.laby.module.ai.framework.agentscope.chat.AiChatCompactionSummarySupport;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 将 Harness Compaction 摘要写入 {@code ai_chat_message}（type=summary）。
 */
@Slf4j
@Service
public class AiChatCompactionSummaryService {

    public static final String MESSAGE_TYPE_SUMMARY = "summary";

    @Resource
    private AgentScopeProperties agentScopeProperties;
    @Resource
    private AiChatMessageMapper chatMessageMapper;

    public boolean isEnabled() {
        return agentScopeProperties.isCompactionSummaryPersist();
    }

    public void persistNewSummaries(HarnessAgent agent, AiChatCompactionSummaryContext context,
                                    Set<String> existingHashes) {
        if (!isEnabled() || agent == null || context == null || context.getConversationId() == null) {
            return;
        }
        List<String> summaries = AiChatCompactionSummarySupport.extractNewSummaries(agent, existingHashes);
        for (String summary : summaries) {
            persistSummary(context, summary);
        }
    }

    private void persistSummary(AiChatCompactionSummaryContext context, String summary) {
        if (chatMessageMapper.existsSummaryContent(context.getConversationId(), summary)) {
            return;
        }
        AiChatMessageDO message = new AiChatMessageDO()
                .setConversationId(context.getConversationId())
                .setUserId(context.getUserId())
                .setRoleId(context.getRoleId())
                .setModelId(context.getModelId())
                .setModel(context.getModel())
                .setType(MESSAGE_TYPE_SUMMARY)
                .setContent(summary)
                .setUseContext(Boolean.FALSE);
        chatMessageMapper.insert(message);
        log.info("[persistSummary][conversationId={}] compaction summary saved, id={}",
                context.getConversationId(), message.getId());
    }

    public static String normalizeSummaryForDisplay(String summary) {
        if (StrUtil.isBlank(summary)) {
            return summary;
        }
        String marker = "A condensed summary follows:";
        int index = summary.indexOf(marker);
        if (index >= 0) {
            return StrUtil.trim(summary.substring(index + marker.length()));
        }
        marker = "Here is a summary of the conversation to date:";
        index = summary.indexOf(marker);
        if (index >= 0) {
            return StrUtil.trim(summary.substring(index + marker.length()));
        }
        return summary;
    }

}
