package com.laby.module.ai.service.knowledge;

import cn.hutool.core.collection.CollUtil;
import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.common.pojo.PageResult;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentPageReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentProcessRespVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentSaveReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentUpdateStatusReqVO;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.dal.mysql.knowledge.AiKnowledgeSegmentMapper;
import com.laby.module.ai.framework.knowledge.retrieval.AiKnowledgeRetrievalService;
import com.laby.module.ai.framework.knowledge.retrieval.bo.AiKnowledgeRetrievalRequest;
import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchResultBO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.framework.common.util.collection.CollectionUtils.convertMap;
import static com.laby.module.ai.enums.ErrorCodeConstants.KNOWLEDGE_RETRIEVAL_DISABLED;

/**
 * AI 知识库分片 Service 实现类（CRUD + 检索委托；索引/向量化见 {@link AiKnowledgeSegmentIndexService}）。
 */
@Service
public class AiKnowledgeSegmentServiceImpl implements AiKnowledgeSegmentService {

    @Resource
    private AiKnowledgeSegmentMapper segmentMapper;
    @Resource
    @Lazy
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource
    @Lazy // 延迟加载，避免与 DocumentService 循环依赖
    private AiKnowledgeSegmentIndexService segmentIndexService;
    @Autowired(required = false)
    private AiKnowledgeRetrievalService knowledgeRetrievalService;

    @Override
    public PageResult<AiKnowledgeSegmentDO> getKnowledgeSegmentPage(AiKnowledgeSegmentPageReqVO pageReqVO) {
        return segmentMapper.selectPage(pageReqVO);
    }

    @Override
    public void createKnowledgeSegmentBySplitContentAsync(Long documentId, String content) {
        segmentIndexService.createKnowledgeSegmentBySplitContentAsync(documentId, content);
    }

    @Override
    public void createKnowledgeSegmentByParseResultAsync(Long documentId, AiStructuredDocumentParseResult parseResult) {
        segmentIndexService.createKnowledgeSegmentByParseResultAsync(documentId, parseResult);
    }

    @Override
    public void createKnowledgeSegmentByParseResult(Long documentId, AiStructuredDocumentParseResult parseResult) {
        segmentIndexService.createKnowledgeSegmentByParseResult(documentId, parseResult);
    }

    @Override
    public void createKnowledgeSegmentBySplitContent(Long documentId, String content) {
        segmentIndexService.createKnowledgeSegmentBySplitContent(documentId, content);
    }

    @Override
    public void updateKnowledgeSegment(AiKnowledgeSegmentSaveReqVO reqVO) {
        segmentIndexService.updateKnowledgeSegment(reqVO);
    }

    @Override
    public void deleteKnowledgeSegment(Long id) {
        segmentIndexService.deleteKnowledgeSegment(id);
    }

    @Override
    public void deleteKnowledgeSegmentByDocumentId(Long documentId) {
        segmentIndexService.deleteKnowledgeSegmentByDocumentId(documentId);
    }

    @Override
    public void updateKnowledgeSegmentStatus(AiKnowledgeSegmentUpdateStatusReqVO reqVO) {
        segmentIndexService.updateKnowledgeSegmentStatus(reqVO);
    }

    @Override
    public void reindexKnowledgeSegmentByKnowledgeId(Long knowledgeId) {
        segmentIndexService.reindexKnowledgeSegmentByKnowledgeId(knowledgeId);
    }

    @Override
    public List<AiKnowledgeSegmentDO> listEnabledSegments(Long knowledgeId) {
        return segmentMapper.selectListByKnowledgeIdAndStatus(knowledgeId, CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public List<AiKnowledgeSegmentDO> listEnabledSegmentsByDocument(Long documentId) {
        return segmentMapper.selectListByDocumentIdAndStatus(documentId, CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public void repairSegmentVector(Long segmentId) {
        segmentIndexService.repairSegmentVector(segmentId);
    }

    @Override
    public void repairSegmentSparseText(Long segmentId) {
        segmentIndexService.repairSegmentSparseText(segmentId);
    }

    @Override
    public List<AiKnowledgeSegmentSearchRespBO> searchKnowledgeSegment(AiKnowledgeSegmentSearchReqBO reqBO) {
        return searchKnowledgeSegmentWithDiagnostics(reqBO).getSegments();
    }

    @Override
    public AiKnowledgeSegmentSearchResultBO searchKnowledgeSegmentWithDiagnostics(AiKnowledgeSegmentSearchReqBO reqBO) {
        AiKnowledgeRetrievalService retrievalService = requireRetrievalService();
        RecallDiagnostics diagnostics = new RecallDiagnostics();
        List<AiKnowledgeSegmentSearchRespBO> segments = retrievalService.retrieve(
                new AiKnowledgeRetrievalRequest()
                        .setKnowledgeId(reqBO.getKnowledgeId())
                        .setQuery(reqBO.getContent())
                        .setTopK(reqBO.getTopK())
                        .setSimilarityThreshold(reqBO.getSimilarityThreshold())
                        .setDiagnostics(diagnostics));
        return new AiKnowledgeSegmentSearchResultBO()
                .setSegments(segments)
                .setDiagnostics(diagnostics);
    }

    @Override
    public List<AiKnowledgeSegmentDO> splitContent(String url, Integer segmentMaxTokens) {
        return segmentIndexService.previewSplitContent(url, segmentMaxTokens);
    }

    @Override
    public List<AiKnowledgeSegmentProcessRespVO> getKnowledgeSegmentProcessList(List<Long> documentIds) {
        if (CollUtil.isEmpty(documentIds)) {
            return Collections.emptyList();
        }
        List<AiKnowledgeSegmentProcessRespVO> processList = segmentMapper.selectProcessList(documentIds);
        Map<Long, AiKnowledgeSegmentProcessRespVO> processMap = convertMap(processList,
                AiKnowledgeSegmentProcessRespVO::getDocumentId);
        Map<Long, AiKnowledgeDocumentDO> documentMap = knowledgeDocumentService.getKnowledgeDocumentMap(documentIds);

        List<AiKnowledgeSegmentProcessRespVO> result = new ArrayList<>(documentIds.size());
        for (Long documentId : documentIds) {
            AiKnowledgeSegmentProcessRespVO vo = processMap.get(documentId);
            if (vo == null) {
                vo = new AiKnowledgeSegmentProcessRespVO();
                vo.setDocumentId(documentId);
                vo.setCount(0L);
                vo.setEmbeddingCount(0L);
            }
            AiKnowledgeDocumentDO document = documentMap.get(documentId);
            if (document != null) {
                vo.setIngestStatus(document.getIngestStatus());
                vo.setIngestError(document.getIngestError());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public Long createKnowledgeSegment(AiKnowledgeSegmentSaveReqVO createReqVO) {
        return segmentIndexService.createKnowledgeSegment(createReqVO);
    }

    @Override
    public AiKnowledgeSegmentDO getKnowledgeSegment(Long id) {
        return segmentMapper.selectById(id);
    }

    @Override
    public List<AiKnowledgeSegmentDO> getKnowledgeSegmentList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return segmentMapper.selectByIds(ids);
    }

    private AiKnowledgeRetrievalService requireRetrievalService() {
        if (knowledgeRetrievalService == null) {
            throw exception(KNOWLEDGE_RETRIEVAL_DISABLED);
        }
        return knowledgeRetrievalService;
    }

}
