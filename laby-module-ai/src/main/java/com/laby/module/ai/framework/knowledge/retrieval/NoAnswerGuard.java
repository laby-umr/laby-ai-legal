package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.ai.enums.knowledge.AiKnowledgeConstants;
import com.laby.module.ai.enums.knowledge.AiRagNoAnswerPolicyEnum;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;

import java.util.List;
import java.util.Optional;

/**
 * 无引用守卫：召回为空或最高分不足时，按策略返回固定回复（不调用 LLM）
 */
public class NoAnswerGuard {

    private final KnowledgeRetrievalProperties properties;

    public NoAnswerGuard(KnowledgeRetrievalProperties properties) {
        this.properties = properties;
    }

    /**
     * @param roleHasKnowledge 当前对话角色是否绑定了知识库
     * @return 需要直接返回的固定文案；空表示可继续调用 LLM
     */
    public Optional<String> evaluate(List<AiKnowledgeSegmentSearchRespBO> segments, boolean roleHasKnowledge) {
        if (!roleHasKnowledge) {
            return Optional.empty();
        }
        if (!shouldBlock(segments)) {
            return Optional.empty();
        }
        AiRagNoAnswerPolicyEnum policy = properties.resolvedNoAnswerPolicy();
        if (policy == AiRagNoAnswerPolicyEnum.RELAXED) {
            return Optional.empty();
        }
        return Optional.of(resolveReply(policy));
    }

    public boolean shouldBlock(List<AiKnowledgeSegmentSearchRespBO> segments) {
        if (CollUtil.isEmpty(segments)) {
            return true;
        }
        Double topScore = segments.get(0).getScore();
        return topScore == null || topScore < properties.getMinAnswerScore();
    }

    private static String resolveReply(AiRagNoAnswerPolicyEnum policy) {
        if (policy == AiRagNoAnswerPolicyEnum.HINT) {
            return AiKnowledgeConstants.NO_ANSWER_HINT_REPLY;
        }
        return AiKnowledgeConstants.NO_ANSWER_STRICT_REPLY;
    }

    public static List<AiKnowledgeSegmentSearchRespBO> filterByMinScore(
            List<AiKnowledgeSegmentSearchRespBO> segments, double minScore) {
        if (CollUtil.isEmpty(segments)) {
            return segments;
        }
        return segments.stream()
                .filter(item -> item.getScore() != null && item.getScore() >= minScore)
                .toList();
    }

}
