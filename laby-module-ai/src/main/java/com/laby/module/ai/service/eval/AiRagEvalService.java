package com.laby.module.ai.service.eval;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.ai.enums.AiRagEvalConstants;
import com.laby.module.ai.service.eval.bo.AiRagEvalCaseBO;
import com.laby.module.ai.service.eval.bo.AiRagEvalReportBO;
import com.laby.module.ai.service.knowledge.AiKnowledgeSegmentService;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 在线 RAG 测评：对真实知识库调用 segment 检索并产出报告。
 */
@Service
public class AiRagEvalService {

    @Resource
    private AiKnowledgeSegmentService knowledgeSegmentService;

    private final AiRagEvalRunner runner = new AiRagEvalRunner();

    public AiRagEvalReportBO runLiveEval(Long knowledgeId, List<AiRagEvalCaseBO> cases) {
        if (knowledgeId == null) {
            throw new IllegalArgumentException("knowledgeId 不能为空");
        }
        List<AiRagEvalCaseBO> targets = CollUtil.isEmpty(cases)
                ? runner.loadCasesFromClasspath(AiRagEvalConstants.LIVE_DATASET)
                : cases;
        for (AiRagEvalCaseBO evalCase : targets) {
            evalCase.setKnowledgeId(knowledgeId);
        }
        return runner.runCases(targets, evalCase -> searchLive(evalCase, knowledgeId));
    }

    private List<AiKnowledgeSegmentSearchRespBO> searchLive(AiRagEvalCaseBO evalCase, Long knowledgeId) {
        AiKnowledgeSegmentSearchReqBO req = new AiKnowledgeSegmentSearchReqBO()
                .setKnowledgeId(knowledgeId)
                .setContent(evalCase.getQuery())
                .setTopK(evalCase.getTopK())
                .setSimilarityThreshold(evalCase.getSimilarityThreshold());
        return knowledgeSegmentService.searchKnowledgeSegment(req);
    }

    public List<AiRagEvalCaseBO> copyLiveCases() {
        return new ArrayList<>(runner.loadCasesFromClasspath(AiRagEvalConstants.LIVE_DATASET));
    }

}
