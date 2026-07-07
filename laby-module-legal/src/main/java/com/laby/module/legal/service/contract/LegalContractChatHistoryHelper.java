package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.ai.core.memory.AgentMemoryPolicy;
import com.laby.module.ai.core.token.AiTokenCounter;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.framework.config.LegalChatMemoryProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 合同问答多轮 history 裁剪与过滤。
 */
@Component
public class LegalContractChatHistoryHelper {

    public static final String MESSAGE_TYPE_SUMMARY = "summary";

    private static final Set<String> SKIP_EXACT = Set.of(
            "未收到模型返回，请重试",
            "流式请求异常，请重试",
            "问答失败，请重试",
            "（模型未返回内容）",
            "已手动停止",
            "连接未建立，请重试",
            "流式响应超时，请重试"
    );

    @Resource
    private LegalChatMemoryProperties memoryProperties;

    @Resource
    private AgentMemoryPolicy agentMemoryPolicy;

    public List<AiMessage> toAiMessages(List<LegalContractChatMessageDO> history) {
        List<AiMessage> messages = new ArrayList<>();
        appendAiHistory(messages, history);
        return messages;
    }

    public void appendAiHistory(List<AiMessage> messages, List<LegalContractChatMessageDO> history) {
        if (CollUtil.isEmpty(history)) {
            return;
        }
        for (LegalContractChatMessageDO turn : filterAndTrim(history)) {
            AiMessage message = toAiMessage(turn);
            if (message != null) {
                messages.add(message);
            }
        }
    }

    public List<LegalContractChatMessageDO> filterAndTrim(List<LegalContractChatMessageDO> history) {
        List<LegalContractChatMessageDO> filtered = filterNoise(history);
        if (CollUtil.isEmpty(filtered)) {
            return List.of();
        }
        return agentMemoryPolicy.trimTail(
                filtered,
                memoryProperties.getHistoryTokenBudget(),
                memoryProperties.getMaxHistoryTurns(),
                LegalContractChatMessageDO::getContent,
                turn -> AiTokenCounter.estimate(truncateContent(turn.getContent())));
    }

    private AiMessage toAiMessage(LegalContractChatMessageDO turn) {
        if (turn == null || StrUtil.isBlank(turn.getContent())) {
            return null;
        }
        String content = truncateContent(turn.getContent());
        AiMessageRoleEnum role = isAssistantTurn(turn) ? AiMessageRoleEnum.ASSISTANT : AiMessageRoleEnum.USER;
        return new AiMessage().setRole(role).setContent(content);
    }

    private static boolean isAssistantTurn(LegalContractChatMessageDO turn) {
        return "assistant".equalsIgnoreCase(turn.getType());
    }

    private static boolean isSummaryTurn(LegalContractChatMessageDO turn) {
        return MESSAGE_TYPE_SUMMARY.equalsIgnoreCase(turn.getType());
    }

    private String truncateContent(String content) {
        return StrUtil.sub(content.trim(), 0, memoryProperties.getMaxTurnChars());
    }

    private List<LegalContractChatMessageDO> filterNoise(List<LegalContractChatMessageDO> history) {
        if (CollUtil.isEmpty(history)) {
            return List.of();
        }
        List<LegalContractChatMessageDO> filtered = new ArrayList<>();
        for (LegalContractChatMessageDO turn : history) {
            if (turn == null || isSummaryTurn(turn) || StrUtil.isBlank(turn.getContent())) {
                continue;
            }
            String content = turn.getContent().trim();
            if (SKIP_EXACT.contains(content)) {
                continue;
            }
            if (content.contains("问答超时") || content.contains("Agent 问答超时")
                    || content.contains("租户上下文异常")) {
                continue;
            }
            filtered.add(turn);
        }
        return filtered;
    }

}
