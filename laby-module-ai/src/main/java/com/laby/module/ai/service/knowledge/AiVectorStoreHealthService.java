package com.laby.module.ai.service.knowledge;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.module.ai.core.rag.AiVectorPointInfo;
import com.laby.module.ai.core.rag.AiVectorStoreClient;
import com.laby.module.ai.core.rag.AiVectorStoreMetadataKeys;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.framework.agentscope.rag.AiVectorStoreHealthProperties;
import com.laby.module.ai.framework.knowledge.retrieval.KnowledgeRetrievalProperties;
import com.laby.module.ai.service.knowledge.bo.AiVectorHealthReportBO;
import com.laby.module.ai.service.model.AiModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库向量健康检查：DB segment 与 Qdrant point 对账，必要时自动 re-embed。
 */
@Slf4j
@Service
public class AiVectorStoreHealthService {

    @Resource
    private AiKnowledgeService knowledgeService;
    @Resource
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private AiKnowledgeSegmentService knowledgeSegmentService;
    @Resource
    private AiModelService modelService;
    @Resource
    private AiVectorStoreHealthProperties healthProperties;
    @Autowired(required = false)
    private KnowledgeRetrievalProperties knowledgeRetrievalProperties;

    /**
     * 检查全部启用知识库；{@code knowledgeId} 非空时仅检查指定库。
     */
    public AiVectorHealthReportBO runHealthCheck(Long knowledgeId, boolean dryRun) {
        return runHealthCheck(knowledgeId, null, dryRun);
    }

    /**
     * 检查向量与 sparse 健康状态；{@code documentId} 非空时仅检查该文档下启用分段。
     */
    public AiVectorHealthReportBO runHealthCheck(Long knowledgeId, Long documentId, boolean dryRun) {
        AiVectorHealthReportBO.AiVectorHealthReportBOBuilder builder = AiVectorHealthReportBO.builder()
                .dryRun(dryRun);
        if (documentId != null) {
            var document = knowledgeDocumentService.getKnowledgeDocument(documentId);
            if (document == null) {
                AiVectorHealthReportBO report = builder.build();
                report.addWarning("文档 " + documentId + " 不存在");
                return report;
            }
            if (knowledgeId != null && !knowledgeId.equals(document.getKnowledgeId())) {
                AiVectorHealthReportBO report = builder.build();
                report.addWarning("documentId=" + documentId + " 不属于 knowledgeId=" + knowledgeId);
                return report;
            }
            knowledgeId = document.getKnowledgeId();
        }
        if (knowledgeId == null) {
            List<AiKnowledgeDO> targets = resolveTargets(null);
            if (CollUtil.isEmpty(targets)) {
                return builder.build();
            }
            return runForKnowledges(targets, null, dryRun, builder);
        }
        AiKnowledgeDO knowledge = knowledgeService.getKnowledge(knowledgeId);
        if (knowledge == null || !CommonStatusEnum.isEnable(knowledge.getStatus())) {
            AiVectorHealthReportBO report = builder.build();
            report.addWarning("知识库 " + knowledgeId + " 不存在或未启用");
            return report;
        }
        return runForKnowledges(List.of(knowledge), documentId, dryRun, builder);
    }

    private AiVectorHealthReportBO runForKnowledges(List<AiKnowledgeDO> targets, Long documentId, boolean dryRun,
                                                    AiVectorHealthReportBO.AiVectorHealthReportBOBuilder builder) {
        int repairsLeft = dryRun ? 0 : healthProperties.getMaxRepairsPerRun();
        AiVectorHealthReportBO aggregate = builder.knowledgeCount(targets.size()).build();
        for (AiKnowledgeDO knowledge : targets) {
            AiVectorHealthReportBO one = checkKnowledge(knowledge, documentId, dryRun, repairsLeft);
            merge(aggregate, one);
            if (!dryRun) {
                repairsLeft = Math.max(0, repairsLeft - one.getRepaired());
            }
            if (!dryRun && repairsLeft <= 0) {
                aggregate.addWarning("已达单次最大修复上限 " + healthProperties.getMaxRepairsPerRun() + "，剩余知识库下次再处理");
                break;
            }
        }
        log.info("[runHealthCheck][dryRun={} documentId={}] scanned={} missingVectorId={} missingInQdrant={} "
                        + "modelMismatch={} missingSparseText={} repaired={} sparseTextRepaired={} failed={}",
                dryRun, documentId, aggregate.getSegmentScanned(), aggregate.getMissingVectorId(),
                aggregate.getMissingInQdrant(), aggregate.getModelMismatch(), aggregate.getMissingSparseText(),
                aggregate.getRepaired(), aggregate.getSparseTextRepaired(), aggregate.getRepairFailed());
        return aggregate;
    }

    private AiVectorHealthReportBO checkKnowledge(AiKnowledgeDO knowledge, Long documentId, boolean dryRun,
                                                  int repairsLeft) {
        AiVectorHealthReportBO report = AiVectorHealthReportBO.builder().knowledgeCount(1).dryRun(dryRun).build();
        if (knowledge.getEmbeddingModelId() == null) {
            report.addWarning("知识库 " + knowledge.getId() + " 未配置 embeddingModelId，已跳过");
            return report;
        }
        List<AiKnowledgeSegmentDO> segments = documentId != null
                ? knowledgeSegmentService.listEnabledSegmentsByDocument(documentId)
                : knowledgeSegmentService.listEnabledSegments(knowledge.getId());
        if (CollUtil.isEmpty(segments)) {
            return report;
        }
        report.setSegmentScanned(segments.size());

        AiVectorStoreClient vectorStoreClient;
        try {
            vectorStoreClient = modelService.getVectorStoreClient(knowledge.getEmbeddingModelId());
        } catch (Exception ex) {
            report.addWarning("知识库 " + knowledge.getId() + " 获取向量客户端失败: " + ex.getMessage());
            return report;
        }

        List<AiKnowledgeSegmentDO> needRepair = new ArrayList<>();
        List<AiKnowledgeSegmentDO> needSparseRepair = new ArrayList<>();
        List<AiKnowledgeSegmentDO> withVectorId = new ArrayList<>();
        for (AiKnowledgeSegmentDO segment : segments) {
            if (isSparseCheckEnabled() && isMissingSparseText(segment)) {
                report.setMissingSparseText(report.getMissingSparseText() + 1);
                needSparseRepair.add(segment);
            }
            if (isMissingVectorId(segment)) {
                report.setMissingVectorId(report.getMissingVectorId() + 1);
                needRepair.add(segment);
                continue;
            }
            withVectorId.add(segment);
        }

        int batchSize = Math.max(1, healthProperties.getBatchSize());
        for (int i = 0; i < withVectorId.size(); i += batchSize) {
            List<AiKnowledgeSegmentDO> batch = withVectorId.subList(i, Math.min(i + batchSize, withVectorId.size()));
            List<String> ids = batch.stream().map(AiKnowledgeSegmentDO::getVectorId).toList();
            List<AiVectorPointInfo> points;
            try {
                points = vectorStoreClient.retrievePoints(ids);
            } catch (Exception ex) {
                report.addWarning("知识库 " + knowledge.getId() + " Qdrant retrieve 失败: " + ex.getMessage());
                continue;
            }
            for (int j = 0; j < batch.size(); j++) {
                AiKnowledgeSegmentDO segment = batch.get(j);
                AiVectorPointInfo point = j < points.size() ? points.get(j) : null;
                if (point == null || !point.isExists()) {
                    report.setMissingInQdrant(report.getMissingInQdrant() + 1);
                    needRepair.add(segment);
                    continue;
                }
                if (isModelMismatch(knowledge, point)) {
                    report.setModelMismatch(report.getModelMismatch() + 1);
                    needRepair.add(segment);
                }
            }
        }

        if (!dryRun && CollUtil.isNotEmpty(needSparseRepair)) {
            for (AiKnowledgeSegmentDO segment : needSparseRepair) {
                try {
                    knowledgeSegmentService.repairSegmentSparseText(segment.getId());
                    report.setSparseTextRepaired(report.getSparseTextRepaired() + 1);
                } catch (Exception ex) {
                    report.setRepairFailed(report.getRepairFailed() + 1);
                    log.warn("[checkKnowledge][segmentId={}] sparse_text 回填失败: {}", segment.getId(), ex.getMessage());
                }
            }
        }

        if (dryRun || CollUtil.isEmpty(needRepair) || repairsLeft <= 0) {
            return report;
        }
        int repaired = 0;
        for (AiKnowledgeSegmentDO segment : needRepair) {
            if (repaired >= repairsLeft) {
                break;
            }
            try {
                knowledgeSegmentService.repairSegmentVector(segment.getId());
                repaired++;
                report.setRepaired(report.getRepaired() + 1);
            } catch (Exception ex) {
                report.setRepairFailed(report.getRepairFailed() + 1);
                log.warn("[checkKnowledge][segmentId={}] 修复失败: {}", segment.getId(), ex.getMessage());
            }
        }
        return report;
    }

    private List<AiKnowledgeDO> resolveTargets(Long knowledgeId) {
        if (knowledgeId != null) {
            AiKnowledgeDO knowledge = knowledgeService.getKnowledge(knowledgeId);
            if (knowledge == null || !CommonStatusEnum.isEnable(knowledge.getStatus())) {
                return List.of();
            }
            return List.of(knowledge);
        }
        return knowledgeService.getKnowledgeSimpleListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private boolean isSparseCheckEnabled() {
        return knowledgeRetrievalProperties != null
                && knowledgeRetrievalProperties.isEnabled()
                && knowledgeRetrievalProperties.getHybrid().isSparseEnabled();
    }

    static boolean isMissingSparseText(AiKnowledgeSegmentDO segment) {
        if (StrUtil.isNotBlank(segment.getSparseText())) {
            return false;
        }
        return StrUtil.isNotBlank(StrUtil.blankToDefault(segment.getEmbedText(), segment.getContent()));
    }

    private static boolean isMissingVectorId(AiKnowledgeSegmentDO segment) {
        return StrUtil.isBlank(segment.getVectorId())
                || AiKnowledgeSegmentDO.VECTOR_ID_EMPTY.equals(segment.getVectorId());
    }

    private static boolean isModelMismatch(AiKnowledgeDO knowledge, AiVectorPointInfo point) {
        if (point.getMetadata() == null) {
            return true;
        }
        String indexedModelId = point.getMetadata().get(AiVectorStoreMetadataKeys.EMBEDDING_MODEL_ID);
        if (StrUtil.isBlank(indexedModelId)) {
            return true;
        }
        return !StrUtil.equals(indexedModelId, String.valueOf(knowledge.getEmbeddingModelId()));
    }

    private static void merge(AiVectorHealthReportBO target, AiVectorHealthReportBO part) {
        target.setSegmentScanned(target.getSegmentScanned() + part.getSegmentScanned());
        target.setMissingVectorId(target.getMissingVectorId() + part.getMissingVectorId());
        target.setMissingInQdrant(target.getMissingInQdrant() + part.getMissingInQdrant());
        target.setModelMismatch(target.getModelMismatch() + part.getModelMismatch());
        target.setMissingSparseText(target.getMissingSparseText() + part.getMissingSparseText());
        target.setRepaired(target.getRepaired() + part.getRepaired());
        target.setSparseTextRepaired(target.getSparseTextRepaired() + part.getSparseTextRepaired());
        target.setRepairFailed(target.getRepairFailed() + part.getRepairFailed());
        if (CollUtil.isNotEmpty(part.getWarnings())) {
            target.getWarnings().addAll(part.getWarnings());
        }
    }

}
