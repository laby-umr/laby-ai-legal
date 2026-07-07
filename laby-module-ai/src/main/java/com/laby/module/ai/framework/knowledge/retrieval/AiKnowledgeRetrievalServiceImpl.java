package com.laby.module.ai.framework.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.core.rag.AiVectorSearchHit;
import com.laby.module.ai.core.rag.AiVectorSearchRequest;
import com.laby.module.ai.core.rag.AiVectorStoreClient;
import com.laby.module.ai.core.rag.AiVectorStoreMetadataKeys;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.dal.mysql.knowledge.AiKnowledgeSegmentMapper;
import com.laby.module.ai.enums.ErrorCodeConstants;
import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import com.laby.module.ai.framework.ai.rerank.AiVectorHitReranker;
import com.laby.module.ai.framework.ai.rerank.DashScopeRerankClient;
import com.laby.module.ai.framework.document.DocumentParseProperties;
import com.laby.module.ai.framework.knowledge.retrieval.bo.AiKnowledgeRetrievalRequest;
import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;
import com.laby.module.ai.service.knowledge.AiKnowledgeService;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.ai.service.knowledge.support.AiKnowledgeSegmentSearchContextSupport;
import com.laby.module.ai.service.model.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.laby.framework.common.util.collection.CollectionUtils.convertList;

/**
 * Universal RAG 检索编排：意图 → 多查询 → Dense/Sparse → RRF → Rerank → 回填
 */
@Slf4j
@RequiredArgsConstructor
public class AiKnowledgeRetrievalServiceImpl implements AiKnowledgeRetrievalService {

    private final KnowledgeRetrievalProperties properties;
    private final AiKnowledgeService knowledgeService;
    private final AiModelService modelService;
    private final AiKnowledgeSegmentMapper segmentMapper;
    private final QueryExpansionService queryExpansionService;
    private final SparseRetrievalEngine sparseRetrievalEngine;
    private final RecallDiagnosticsCollector diagnosticsCollector;
    private final DocumentParseProperties documentParseProperties;

    @Autowired(required = false)
    private DashScopeRerankClient dashScopeRerankClient;

    @Override
    public List<AiKnowledgeSegmentSearchRespBO> retrieve(AiKnowledgeRetrievalRequest request) {
        long startedAtMs = System.currentTimeMillis();
        RecallDiagnostics diagnostics = request.getDiagnostics();
        if (diagnostics == null) {
            diagnostics = diagnosticsCollector.start();
            request.setDiagnostics(diagnostics);
        }

        AiKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(request.getKnowledgeId());
        String query = StrUtil.trim(request.getQuery());
        if (StrUtil.isBlank(query)) {
            diagnosticsCollector.finish(request.getKnowledgeId(), query, diagnostics, startedAtMs);
            return List.of();
        }

        int topK = ObjUtil.defaultIfNull(request.getTopK(), ObjUtil.defaultIfNull(knowledge.getTopK(), properties.getDefaultTopK()));
        double similarityThreshold = ObjUtil.defaultIfNull(request.getSimilarityThreshold(),
                ObjUtil.defaultIfNull(knowledge.getSimilarityThreshold(), properties.getDefaultSimilarityThreshold()));
        int searchTopK = Math.min(40, topK * properties.getRetrievalFactor());

        AiQueryIntentEnum intent = request.getIntent() != null
                ? request.getIntent()
                : QueryIntentClassifier.classify(query);
        diagnosticsCollector.recordIntent(diagnostics, intent);

        List<String> variants = resolveVariants(request, query, intent);
        diagnosticsCollector.recordVariants(diagnostics, variants);

        KnowledgeRetrievalProperties.HybridConfig hybrid = properties.getHybrid();
        List<RrfFusion.WeightedRankedList> rankedLists = new ArrayList<>();
        int denseHitCount = 0;
        int sparseHitCount = 0;

        boolean hybridEnabled = request.isEnableHybrid() && hybrid.isEnabled();
        for (String variant : variants) {
            List<Long> denseIds = searchDenseSegmentIds(knowledge, request.getKnowledgeId(), variant, searchTopK);
            denseHitCount += denseIds.size();
            if (CollUtil.isNotEmpty(denseIds)) {
                rankedLists.add(new RrfFusion.WeightedRankedList(hybrid.getDenseWeight(), denseIds));
            }
            if (hybridEnabled && hybrid.isSparseEnabled()) {
                List<Long> sparseIds = sparseRetrievalEngine.search(request.getKnowledgeId(), variant, searchTopK, diagnostics);
                sparseHitCount += sparseIds.size();
                if (CollUtil.isNotEmpty(sparseIds)) {
                    rankedLists.add(new RrfFusion.WeightedRankedList(hybrid.getSparseWeight(), sparseIds));
                }
            }
        }
        diagnosticsCollector.recordDenseHits(diagnostics, denseHitCount);
        diagnosticsCollector.recordSparseHits(diagnostics, sparseHitCount);

        Map<Long, Double> fusedScores = RrfFusion.fuse(hybrid.getRrfK(), rankedLists);
        if (CollUtil.isEmpty(fusedScores)) {
            diagnosticsCollector.finish(request.getKnowledgeId(), query, diagnostics, startedAtMs);
            return List.of();
        }
        diagnosticsCollector.recordFusedHits(diagnostics, fusedScores.size());

        List<Long> fusedSegmentIds = new ArrayList<>(fusedScores.keySet());
        List<AiKnowledgeSegmentDO> segments = segmentMapper.selectListByIds(fusedSegmentIds);
        Map<Long, AiKnowledgeSegmentDO> segmentMap = segments.stream()
                .collect(Collectors.toMap(AiKnowledgeSegmentDO::getId, Function.identity(), (a, b) -> a));

        Map<Long, Double> boostedScores = applyBlockBoost(fusedScores, segmentMap, intent);
        List<ScoredSegment> candidates = boostedScores.entrySet().stream()
                .map(entry -> new ScoredSegment(segmentMap.get(entry.getKey()), entry.getValue()))
                .filter(item -> item.segment() != null)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(searchTopK)
                .toList();

        List<AiKnowledgeSegmentSearchRespBO> results = rerankAndBuild(query, candidates, topK, similarityThreshold, diagnostics);
        results = NoAnswerGuard.filterByMinScore(results, properties.getMinAnswerScore());

        if (CollUtil.isNotEmpty(results)) {
            segmentMapper.updateRetrievalCountIncrByIds(convertList(results, AiKnowledgeSegmentSearchRespBO::getId));
        }

        Double topScore = CollUtil.isEmpty(results) ? null : results.get(0).getScore();
        diagnosticsCollector.recordRerankHits(diagnostics, results.size(), topScore);
        diagnosticsCollector.finish(request.getKnowledgeId(), query, diagnostics, startedAtMs);

        if (CollUtil.isEmpty(results)) {
            return List.of();
        }
        List<AiKnowledgeSegmentDO> hitSegments = segmentMapper.selectListByIds(
                convertList(results, AiKnowledgeSegmentSearchRespBO::getId));
        return enrichSearchResults(results, hitSegments);
    }

    private List<String> resolveVariants(AiKnowledgeRetrievalRequest request, String query, AiQueryIntentEnum intent) {
        if (!request.isEnableMultiQuery() || !properties.getMultiQuery().isEnabled()) {
            return List.of(query);
        }
        List<String> variants = queryExpansionService.expand(query, intent);
        return CollUtil.isEmpty(variants) ? List.of(query) : variants;
    }

    private List<Long> searchDenseSegmentIds(AiKnowledgeDO knowledge, Long knowledgeId, String query, int searchTopK) {
        AiVectorStoreClient vectorStoreClient = modelService.getVectorStoreClient(knowledge.getEmbeddingModelId());
        AiVectorSearchRequest searchRequest = new AiVectorSearchRequest()
                .setQuery(query)
                .setTopK(searchTopK)
                .setMetadataEquals(AiVectorStoreMetadataKeys.knowledgeSearchFilter(knowledgeId));
        List<AiVectorSearchHit> hits = vectorStoreClient.search(searchRequest);
        if (CollUtil.isEmpty(hits)) {
            return List.of();
        }
        List<AiKnowledgeSegmentDO> segments = segmentMapper.selectListByVectorIds(convertList(hits, AiVectorSearchHit::getId));
        if (CollUtil.isEmpty(segments)) {
            return List.of();
        }
        Map<String, Long> vectorToSegmentId = segments.stream()
                .filter(item -> StrUtil.isNotBlank(item.getVectorId()))
                .collect(Collectors.toMap(AiKnowledgeSegmentDO::getVectorId, AiKnowledgeSegmentDO::getId, (a, b) -> a));
        List<Long> segmentIds = new ArrayList<>(hits.size());
        for (AiVectorSearchHit hit : hits) {
            Long segmentId = vectorToSegmentId.get(hit.getId());
            if (segmentId != null) {
                segmentIds.add(segmentId);
            }
        }
        return segmentIds;
    }

    private Map<Long, Double> applyBlockBoost(Map<Long, Double> fusedScores,
                                              Map<Long, AiKnowledgeSegmentDO> segmentMap,
                                              AiQueryIntentEnum intent) {
        KnowledgeRetrievalProperties.BlockRouteConfig blockRoute = properties.getBlockRoute();
        if (!blockRoute.isEnabled()) {
            return fusedScores;
        }
        Map<Long, Double> boosted = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : fusedScores.entrySet()) {
            AiKnowledgeSegmentDO segment = segmentMap.get(entry.getKey());
            String blockType = segment != null ? segment.getBlockType() : null;
            boosted.put(entry.getKey(), BlockTypeRouteBoost.apply(intent, blockType, entry.getValue(), blockRoute));
        }
        return boosted;
    }

    private List<AiKnowledgeSegmentSearchRespBO> rerankAndBuild(String query,
                                                                List<ScoredSegment> candidates,
                                                                int topK,
                                                                double similarityThreshold,
                                                                RecallDiagnostics diagnostics) {
        if (CollUtil.isEmpty(candidates)) {
            return List.of();
        }
        boolean rerankEnabled = properties.getRerank().isEnabled() && dashScopeRerankClient != null;
        if (!rerankEnabled) {
            if (properties.getRerank().isEnabled() && dashScopeRerankClient == null && diagnostics != null) {
                diagnosticsCollector.recordDegrade(diagnostics, ErrorCodeConstants.KNOWLEDGE_RETRIEVAL_RERANK_UNAVAILABLE);
            }
            // RRF 分值约在 0.01～0.05，不能与 cosine/rerank 阈值（如 0.60）直接比较
            List<ScoredSegment> topCandidates = candidates.stream().limit(topK).toList();
            if (CollUtil.isEmpty(topCandidates)) {
                return List.of();
            }
            double maxRrfScore = topCandidates.stream().mapToDouble(ScoredSegment::score).max().orElse(0);
            return topCandidates.stream()
                    .map(item -> toRespBo(item.segment(), RrfScoreNormalizer.normalizeToDisplayScore(item.score(), maxRrfScore)))
                    .toList();
        }

        List<AiVectorSearchHit> hits = candidates.stream()
                .map(item -> new AiVectorSearchHit()
                        .setId(item.segment().getVectorId())
                        .setScore(item.score())
                        .setContent(StrUtil.blankToDefault(item.segment().getEmbedText(), item.segment().getContent())))
                .toList();
        List<AiVectorSearchHit> rerankedHits = AiVectorHitReranker.rerank(
                dashScopeRerankClient, query, hits, topK, similarityThreshold);
        if (CollUtil.isEmpty(rerankedHits)) {
            return List.of();
        }

        Map<String, ScoredSegment> vectorCandidateMap = candidates.stream()
                .filter(item -> StrUtil.isNotBlank(item.segment().getVectorId()))
                .collect(Collectors.toMap(item -> item.segment().getVectorId(), Function.identity(), (a, b) -> a));

        List<AiKnowledgeSegmentSearchRespBO> results = new ArrayList<>(rerankedHits.size());
        for (AiVectorSearchHit hit : rerankedHits) {
            ScoredSegment candidate = vectorCandidateMap.get(hit.getId());
            if (candidate == null) {
                continue;
            }
            results.add(toRespBo(candidate.segment(), hit.getScore()));
        }
        return results;
    }

    private AiKnowledgeSegmentSearchRespBO toRespBo(AiKnowledgeSegmentDO segment, Double score) {
        return BeanUtils.toBean(segment, AiKnowledgeSegmentSearchRespBO.class).setScore(score);
    }

    private List<AiKnowledgeSegmentSearchRespBO> enrichSearchResults(List<AiKnowledgeSegmentSearchRespBO> hits,
                                                                     List<AiKnowledgeSegmentDO> hitSegments) {
        boolean parentBackfill = documentParseProperties.getStructuredChunk().isParentBackfillEnabled();
        if (!parentBackfill) {
            return hits;
        }
        return AiKnowledgeSegmentSearchContextSupport.enrich(hits, hitSegments,
                ids -> CollUtil.isEmpty(ids) ? List.of() : segmentMapper.selectListByIds(ids),
                (documentId, headingPath) -> segmentMapper.selectTableWholeSegment(documentId, headingPath),
                parentBackfill);
    }

    private record ScoredSegment(AiKnowledgeSegmentDO segment, double score) {
    }

}
