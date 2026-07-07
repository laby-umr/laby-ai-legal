package com.laby.module.ai.service.knowledge.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 结构化 RAG 检索上下文增强（Parent 回填 + 表格整表附带）
 */
public final class AiKnowledgeSegmentSearchContextSupport {

    private static final String EXPANDED_SEPARATOR = "\n---\n";

    private AiKnowledgeSegmentSearchContextSupport() {
    }

    public static List<AiKnowledgeSegmentSearchRespBO> enrich(List<AiKnowledgeSegmentSearchRespBO> hits,
                                                              List<AiKnowledgeSegmentDO> hitSegments,
                                                              Function<Set<Long>, List<AiKnowledgeSegmentDO>> segmentLoader,
                                                              BiFunction<Long, String, AiKnowledgeSegmentDO> tableWholeResolver,
                                                              boolean parentBackfillEnabled) {
        if (CollUtil.isEmpty(hits) || CollUtil.isEmpty(hitSegments)) {
            return hits;
        }
        Map<Long, AiKnowledgeSegmentDO> hitSegmentMap = hitSegments.stream()
                .collect(Collectors.toMap(AiKnowledgeSegmentDO::getId, Function.identity(), (a, b) -> a));

        Set<Long> loadIds = new HashSet<>();
        if (parentBackfillEnabled) {
            hitSegments.stream()
                    .map(AiKnowledgeSegmentDO::getParentId)
                    .filter(Objects::nonNull)
                    .forEach(loadIds::add);
        }
        Map<Long, AiKnowledgeSegmentDO> relatedMap = new HashMap<>();
        if (tableWholeResolver != null) {
            for (AiKnowledgeSegmentDO segment : hitSegments) {
                if (!AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW.getCode().equals(segment.getBlockType())) {
                    continue;
                }
                AiKnowledgeSegmentDO tableWhole = tableWholeResolver.apply(
                        segment.getDocumentId(), segment.getHeadingPath());
                if (tableWhole != null && tableWhole.getId() != null) {
                    relatedMap.put(tableWhole.getId(), tableWhole);
                }
            }
        }
        if (CollUtil.isNotEmpty(loadIds)) {
            List<AiKnowledgeSegmentDO> related = segmentLoader.apply(loadIds);
            if (CollUtil.isNotEmpty(related)) {
                related.forEach(item -> relatedMap.put(item.getId(), item));
            }
        }

        List<AiKnowledgeSegmentSearchRespBO> enriched = new ArrayList<>(hits.size());
        for (AiKnowledgeSegmentSearchRespBO hit : hits) {
            AiKnowledgeSegmentDO segment = hitSegmentMap.get(hit.getId());
            if (segment == null) {
                enriched.add(hit);
                continue;
            }
            AiKnowledgeSegmentSearchRespBO copy = copyHit(hit, segment);
            copy.setExpandedContent(buildExpandedContent(segment, relatedMap, parentBackfillEnabled));
            enriched.add(copy);
        }
        return enriched;
    }

    private static String buildExpandedContent(AiKnowledgeSegmentDO segment,
                                               Map<Long, AiKnowledgeSegmentDO> relatedMap,
                                               boolean parentBackfillEnabled) {
        List<String> parts = new ArrayList<>();
        if (parentBackfillEnabled && segment.getParentId() != null) {
            AiKnowledgeSegmentDO parent = relatedMap.get(segment.getParentId());
            if (parent != null && StrUtil.isNotBlank(parent.getContent())) {
                parts.add(parent.getContent());
            }
        }
        if (AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW.getCode().equals(segment.getBlockType())) {
            relatedMap.values().stream()
                    .filter(item -> Objects.equals(item.getDocumentId(), segment.getDocumentId()))
                    .filter(item -> AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE.getCode().equals(item.getBlockType()))
                    .filter(item -> StrUtil.equals(item.getHeadingPath(), segment.getHeadingPath()))
                    .map(AiKnowledgeSegmentDO::getContent)
                    .findFirst()
                    .ifPresent(parts::add);
        }
        parts.add(StrUtil.blankToDefault(segment.getContent(), ""));
        return parts.stream().filter(StrUtil::isNotBlank).collect(Collectors.joining(EXPANDED_SEPARATOR));
    }

    private static AiKnowledgeSegmentSearchRespBO copyHit(AiKnowledgeSegmentSearchRespBO hit,
                                                          AiKnowledgeSegmentDO segment) {
        AiKnowledgeSegmentSearchRespBO copy = new AiKnowledgeSegmentSearchRespBO();
        copy.setId(hit.getId());
        copy.setDocumentId(hit.getDocumentId());
        copy.setKnowledgeId(hit.getKnowledgeId());
        copy.setContent(hit.getContent());
        copy.setContentLength(hit.getContentLength());
        copy.setTokens(hit.getTokens());
        copy.setScore(hit.getScore());
        copy.setParentId(segment.getParentId());
        copy.setBlockType(segment.getBlockType());
        copy.setHeadingPath(segment.getHeadingPath());
        if (segment.getParentId() != null) {
            copy.setChunkLevel(segment.getChunkLevel());
        } else {
            copy.setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD.getLevel());
        }
        return copy;
    }

}
