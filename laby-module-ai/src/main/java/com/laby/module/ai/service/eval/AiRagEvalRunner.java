package com.laby.module.ai.service.eval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.ai.enums.AiRagEvalConstants;
import com.laby.module.ai.service.eval.bo.AiRagEvalCaseBO;
import com.laby.module.ai.service.eval.bo.AiRagEvalCaseResultBO;
import com.laby.module.ai.service.eval.bo.AiRagEvalExpectationBO;
import com.laby.module.ai.service.eval.bo.AiRagEvalReportBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RAG 检索黄金集评测（Hit@K / MRR / Recall@K）
 */
public class AiRagEvalRunner {

    public AiRagEvalReportBO runFixtureDataset() {
        return runFromClasspath(AiRagEvalConstants.FIXTURE_DATASET, new InMemoryRagEvalRetriever());
    }

    public AiRagEvalReportBO runPdfStructuredDataset() {
        return runFromClasspath(AiRagEvalConstants.PDF_STRUCTURED_DATASET, new InMemoryRagEvalRetriever());
    }

    public AiRagEvalReportBO runExcelDataset() {
        return runFromClasspath(AiRagEvalConstants.EXCEL_DATASET, new InMemoryRagEvalRetriever());
    }

    public AiRagEvalReportBO runFuzzyQueryDataset() {
        return runFromClasspath(AiRagEvalConstants.FUZZY_QUERY_DATASET, new InMemoryRagEvalRetriever());
    }

    public AiRagEvalReportBO runFromClasspath(String resourcePath, AiRagEvalRetriever retriever) {
        return runCases(loadCases(resourcePath), retriever);
    }

    public List<AiRagEvalCaseBO> loadCasesFromClasspath(String resourcePath) {
        return loadCases(resourcePath);
    }

    public AiRagEvalReportBO runCases(List<AiRagEvalCaseBO> cases, AiRagEvalRetriever retriever) {
        List<AiRagEvalCaseResultBO> results = new ArrayList<>();
        int passed = 0;
        int hitAtK = 0;
        double mrrSum = 0D;
        double recallSum = 0D;
        List<String> failedIds = new ArrayList<>();
        for (AiRagEvalCaseBO evalCase : cases) {
            AiRagEvalCaseResultBO one = evaluateCase(evalCase, retriever);
            results.add(one);
            if (one.isPass()) {
                passed++;
            } else {
                failedIds.add(evalCase.getCaseId());
            }
            if (one.isHitAtK()) {
                hitAtK++;
            }
            mrrSum += one.getMrr();
            recallSum += one.getRecallAtK();
        }
        int total = cases.size();
        return AiRagEvalReportBO.builder()
                .totalCases(total)
                .passedCases(passed)
                .failedCaseIds(failedIds)
                .caseResults(results)
                .hitAtKCases(hitAtK)
                .avgMrr(total == 0 ? 0D : mrrSum / total)
                .hitAtKRate(total == 0 ? 0D : (double) hitAtK / total)
                .avgRecallAtK(total == 0 ? 0D : recallSum / total)
                .build();
    }

    AiRagEvalCaseResultBO evaluateCase(AiRagEvalCaseBO evalCase, AiRagEvalRetriever retriever) {
        List<AiKnowledgeSegmentSearchRespBO> retrieved = retriever.search(evalCase);
        List<Long> retrievedIds = retrieved.stream()
                .map(AiKnowledgeSegmentSearchRespBO::getId)
                .filter(id -> id != null)
                .toList();
        Double topScore = CollUtil.isEmpty(retrieved) ? null : retrieved.get(0).getScore();

        AiRagEvalExpectationBO expectation = evalCase.getExpectation();
        Set<Long> expectedIds = expectation != null && CollUtil.isNotEmpty(expectation.getExpectedSegmentIds())
                ? new HashSet<>(expectation.getExpectedSegmentIds()) : Set.of();

        int firstHitRank = findFirstHitRank(retrievedIds, expectedIds);
        boolean hit = firstHitRank > 0;
        double mrr = hit ? 1D / firstHitRank : 0D;
        double recallAtK = computeRecallAtK(retrievedIds, expectedIds);
        double minRecall = resolveMinRecallAtK(expectation, expectedIds);
        boolean contentOk = assertContentContains(retrieved, expectation);
        boolean scoreOk = expectation == null || expectation.getMinTopScore() == null
                || (topScore != null && topScore >= expectation.getMinTopScore());

        boolean pass = contentOk && scoreOk;
        if (CollUtil.isNotEmpty(expectedIds)) {
            pass = pass && hit && recallAtK + 1e-9 >= minRecall;
        }
        String failureReason = null;
        if (!pass) {
            failureReason = buildFailureReason(hit, recallAtK, minRecall, contentOk, scoreOk, topScore, expectation);
        }
        return AiRagEvalCaseResultBO.builder()
                .caseId(evalCase.getCaseId())
                .description(evalCase.getDescription())
                .pass(pass)
                .hitAtK(hit)
                .mrr(mrr)
                .recallAtK(recallAtK)
                .topScore(topScore)
                .retrievedSegmentIds(retrievedIds)
                .failureReason(failureReason)
                .build();
    }

    private static List<AiRagEvalCaseBO> loadCases(String resourcePath) {
        try (InputStream in = AiRagEvalRunner.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("RAG 评测资源不存在: " + resourcePath);
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtils.parseObject(json, new TypeReference<List<AiRagEvalCaseBO>>() {});
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("加载 RAG 评测集失败: " + resourcePath, ex);
        }
    }

    private static int findFirstHitRank(List<Long> retrievedIds, Set<Long> expectedIds) {
        if (CollUtil.isEmpty(expectedIds)) {
            return 0;
        }
        for (int i = 0; i < retrievedIds.size(); i++) {
            if (expectedIds.contains(retrievedIds.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    private static double computeRecallAtK(List<Long> retrievedIds, Set<Long> expectedIds) {
        if (CollUtil.isEmpty(expectedIds)) {
            return 1D;
        }
        int hit = 0;
        for (Long id : retrievedIds) {
            if (expectedIds.contains(id)) {
                hit++;
            }
        }
        return (double) hit / expectedIds.size();
    }

    private static double resolveMinRecallAtK(AiRagEvalExpectationBO expectation, Set<Long> expectedIds) {
        if (expectation != null && expectation.getMinRecallAtK() != null) {
            return expectation.getMinRecallAtK();
        }
        return CollUtil.isEmpty(expectedIds) ? 0D : 0.5D;
    }

    private static boolean assertContentContains(List<AiKnowledgeSegmentSearchRespBO> retrieved,
                                                 AiRagEvalExpectationBO expectation) {
        if (expectation == null || CollUtil.isEmpty(expectation.getExpectedContentContains())) {
            return true;
        }
        for (String keyword : expectation.getExpectedContentContains()) {
            if (StrUtil.isBlank(keyword)) {
                continue;
            }
            boolean hit = retrieved.stream()
                    .map(AiKnowledgeSegmentSearchRespBO::getContent)
                    .filter(StrUtil::isNotBlank)
                    .anyMatch(content -> StrUtil.contains(content, keyword));
            if (hit) {
                return true;
            }
        }
        return false;
    }

    private static String buildFailureReason(boolean hit, double recallAtK, double minRecall,
                                               boolean contentOk, boolean scoreOk, Double topScore,
                                               AiRagEvalExpectationBO expectation) {
        List<String> parts = new ArrayList<>();
        if (!hit) {
            parts.add("Hit@K 未命中");
        }
        if (recallAtK + 1e-9 < minRecall) {
            parts.add(String.format("Recall@K=%.2f < %.2f", recallAtK, minRecall));
        }
        if (!contentOk) {
            parts.add("TopK 未包含期望关键词");
        }
        if (!scoreOk) {
            parts.add(String.format("Top1 分数 %.3f < %.3f", topScore, expectation.getMinTopScore()));
        }
        return String.join("; ", parts);
    }

}
