package com.laby.module.ai.service.eval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.service.eval.bo.AiRagEvalCaseBO;
import com.laby.module.ai.service.eval.bo.AiRagEvalSegmentFixtureBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 离线 RAG 测评检索器：基于 query 与语料片段的关键词重叠打分，不依赖 Embedding / Qdrant。
 */
public class InMemoryRagEvalRetriever implements AiRagEvalRetriever {

    @Override
    public List<AiKnowledgeSegmentSearchRespBO> search(AiRagEvalCaseBO evalCase) {
        if (CollUtil.isEmpty(evalCase.getSegments())) {
            return List.of();
        }
        Set<String> queryTokens = tokenize(evalCase.getQuery());
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        int topK = evalCase.getTopK() != null ? evalCase.getTopK() : 5;
        List<AiKnowledgeSegmentSearchRespBO> scored = new ArrayList<>();
        for (AiRagEvalSegmentFixtureBO fixture : evalCase.getSegments()) {
            if (fixture == null || StrUtil.isBlank(fixture.getContent())) {
                continue;
            }
            double score = overlapScore(queryTokens, tokenize(fixture.getContent()));
            if (score <= 0D) {
                continue;
            }
            AiKnowledgeSegmentSearchRespBO row = new AiKnowledgeSegmentSearchRespBO();
            row.setId(fixture.getId());
            row.setContent(fixture.getContent());
            row.setScore(score);
            scored.add(row);
        }
        scored.sort(Comparator.comparing(AiKnowledgeSegmentSearchRespBO::getScore).reversed());
        return scored.size() <= topK ? scored : scored.subList(0, topK);
    }

    private static double overlapScore(Set<String> queryTokens, Set<String> contentTokens) {
        if (CollUtil.isEmpty(queryTokens) || CollUtil.isEmpty(contentTokens)) {
            return 0D;
        }
        int hit = 0;
        for (String token : queryTokens) {
            if (contentTokens.contains(token)) {
                hit++;
            }
        }
        return (double) hit / queryTokens.size();
    }

    static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (StrUtil.isBlank(text)) {
            return tokens;
        }
        String normalized = StrUtil.trim(text).toLowerCase(Locale.ROOT);
        for (String part : normalized.split("[\\s,，。；;、！!？?（）()\\[\\]【】\"'\\-]+")) {
            if (StrUtil.isNotBlank(part)) {
                tokens.add(part);
            }
        }
        // 中文连续字串补充 2-gram，提升短 query 召回
        String compact = normalized.replaceAll("\\s+", "");
        for (int i = 0; i < compact.length() - 1; i++) {
            tokens.add(compact.substring(i, i + 2));
        }
        return tokens;
    }

}
