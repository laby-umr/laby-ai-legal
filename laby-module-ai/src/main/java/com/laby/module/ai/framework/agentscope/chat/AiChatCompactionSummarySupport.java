package com.laby.module.ai.framework.agentscope.chat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.ConversationCompactor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 从 Harness Agent 状态中提取 Compaction 摘要消息。
 */
public final class AiChatCompactionSummarySupport {

    private AiChatCompactionSummarySupport() {
    }

    public static Set<String> snapshotSummaryHashes(HarnessAgent agent) {
        Set<String> hashes = new LinkedHashSet<>();
        for (String text : extractSummaryTexts(agent)) {
            hashes.add(hash(text));
        }
        return hashes;
    }

    public static List<String> extractNewSummaries(HarnessAgent agent, Set<String> existingHashes) {
        return extractSummaryTexts(agent).stream()
                .filter(text -> !existingHashes.contains(hash(text)))
                .toList();
    }

    public static List<String> extractSummaryTexts(HarnessAgent agent) {
        if (agent == null || agent.getAgentState() == null) {
            return List.of();
        }
        List<Msg> messages = agent.getAgentState().contextMutable();
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .filter(msg -> ConversationCompactor.SUMMARY_MSG_NAME.equals(msg.getName()))
                .map(AiChatCompactionSummarySupport::extractText)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private static String extractText(Msg msg) {
        if (msg.getContent() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : msg.getContent()) {
            if (block instanceof TextBlock textBlock && StrUtil.isNotBlank(textBlock.getText())) {
                builder.append(textBlock.getText().trim());
            }
        }
        return builder.toString().trim();
    }

    public static String hash(String text) {
        return DigestUtil.sha256Hex(StrUtil.nullToDefault(text, ""));
    }

}
