package com.laby.module.ai.service.knowledge;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentSaveReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentUpdateStatusReqVO;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.core.rag.AiVectorStoreClient;
import com.laby.module.ai.core.token.AiTokenCounter;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.dal.mysql.knowledge.AiKnowledgeSegmentMapper;
import com.laby.module.ai.enums.AiDocumentSplitStrategyEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentIngestStatusEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import com.laby.module.ai.framework.document.DocumentParseProperties;
import com.laby.module.ai.framework.document.DocumentTypeChunkRouter;
import com.laby.module.ai.framework.document.DocumentTypeResolver;
import com.laby.module.ai.framework.knowledge.retrieval.SparseTextNormalizer;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeChunkBO;
import com.laby.module.ai.service.knowledge.support.AiKnowledgeSegmentSplitSupport;
import com.laby.module.ai.service.knowledge.support.AiKnowledgeSegmentVectorSupport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.framework.common.util.collection.CollectionUtils.convertList;
import static com.laby.module.ai.enums.ErrorCodeConstants.KNOWLEDGE_SEGMENT_CONTENT_TOO_LONG;
import static com.laby.module.ai.enums.ErrorCodeConstants.KNOWLEDGE_SEGMENT_NOT_EXISTS;
import static com.laby.module.ai.service.knowledge.support.AiKnowledgeSegmentVectorSupport.listEmbeddedVectorIds;
import static com.laby.module.ai.service.knowledge.support.AiKnowledgeSegmentVectorSupport.resolveEmbedContent;

/**
 * 知识库分段索引：切片、向量化、重建索引。
 */
@Service
@Slf4j
public class AiKnowledgeSegmentIndexService {

    @Resource
    private AiKnowledgeSegmentMapper segmentMapper;

    @Resource
    private AiKnowledgeService knowledgeService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private AiKnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private DocumentParseProperties documentParseProperties;

    @Resource
    private DocumentTypeChunkRouter documentTypeChunkRouter;

    @Resource
    private AiKnowledgeSegmentVectorSupport vectorSupport;

    @Resource
    private AiKnowledgeSegmentSplitSupport splitSupport;

    @Async
    public void createKnowledgeSegmentBySplitContentAsync(Long documentId, String content) {
        try {
            knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                    AiKnowledgeDocumentIngestStatusEnum.SPLITTING.getStatus(), null);
            createKnowledgeSegmentBySplitContent(documentId, content);
        } catch (Throwable ex) {
            log.error("[createKnowledgeSegmentBySplitContentAsync][documentId({}) 入库失败]", documentId, ex);
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                    AiKnowledgeDocumentIngestStatusEnum.FAILED.getStatus(), message);
        }
    }

    @Async
    public void createKnowledgeSegmentByParseResultAsync(Long documentId, AiStructuredDocumentParseResult parseResult) {
        try {
            knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                    AiKnowledgeDocumentIngestStatusEnum.SPLITTING.getStatus(), null);
            createKnowledgeSegmentByParseResult(documentId, parseResult);
        } catch (Throwable ex) {
            log.error("[createKnowledgeSegmentByParseResultAsync][documentId({}) 入库失败]", documentId, ex);
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                    AiKnowledgeDocumentIngestStatusEnum.FAILED.getStatus(), message);
        }
    }

    public void createKnowledgeSegmentByParseResult(Long documentId, AiStructuredDocumentParseResult parseResult) {
        AiKnowledgeDocumentDO documentDO = knowledgeDocumentService.validateKnowledgeDocumentExists(documentId);
        AiKnowledgeDO knowledgeDO = knowledgeService.validateKnowledgeExists(documentDO.getKnowledgeId());
        String documentType = StrUtil.blankToDefault(documentDO.getDocumentType(),
                DocumentTypeResolver.resolveFromUrl(documentDO.getUrl(), parseResult).getCode());
        List<AiKnowledgeChunkBO> chunkBOs = documentTypeChunkRouter.chunk(
                documentType, parseResult, documentDO.getSegmentMaxTokens());
        if (CollUtil.isEmpty(chunkBOs)) {
            knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                    AiKnowledgeDocumentIngestStatusEnum.SUCCESS.getStatus(), null);
            return;
        }
        AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(knowledgeDO);
        persistStructuredChunks(documentId, documentDO, vectorStoreClient, parseResult, chunkBOs);
    }

    private void persistStructuredChunks(Long documentId,
                                         AiKnowledgeDocumentDO documentDO,
                                         AiVectorStoreClient vectorStoreClient,
                                         AiStructuredDocumentParseResult parseResult,
                                         List<AiKnowledgeChunkBO> chunkBOs) {
        Map<String, Long> parentTempKeyToId = new HashMap<>();
        List<AiKnowledgeSegmentDO> parents = new ArrayList<>();
        List<AiKnowledgeChunkBO> parentChunkBOs = new ArrayList<>();
        List<AiKnowledgeChunkBO> childChunkBOs = new ArrayList<>();

        for (AiKnowledgeChunkBO chunkBO : chunkBOs) {
            if (chunkBO.getChunkLevel() == AiKnowledgeSegmentChunkLevelEnum.PARENT) {
                parentChunkBOs.add(chunkBO);
            } else {
                childChunkBOs.add(chunkBO);
            }
        }

        for (AiKnowledgeChunkBO chunkBO : parentChunkBOs) {
            AiKnowledgeSegmentDO parent = toSegmentDO(documentDO.getKnowledgeId(), documentId, chunkBO);
            segmentMapper.insert(parent);
            parents.add(parent);
            if (StrUtil.isNotBlank(chunkBO.getTempKey())) {
                parentTempKeyToId.put(chunkBO.getTempKey(), parent.getId());
            }
        }

        List<AiKnowledgeSegmentDO> children = new ArrayList<>();
        for (AiKnowledgeChunkBO chunkBO : childChunkBOs) {
            AiKnowledgeSegmentDO child = toSegmentDO(documentDO.getKnowledgeId(), documentId, chunkBO);
            if (StrUtil.isNotBlank(chunkBO.getParentTempKey())) {
                child.setParentId(parentTempKeyToId.get(chunkBO.getParentTempKey()));
            }
            children.add(child);
        }
        if (CollUtil.isNotEmpty(children)) {
            segmentMapper.insertBatch(children);
        }

        knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                AiKnowledgeDocumentIngestStatusEnum.EMBEDDING.getStatus(), null);

        List<AiKnowledgeSegmentDO> allEmbedCandidates = new ArrayList<>();
        if (documentParseProperties.getStructuredChunk().isEmbedParent()) {
            allEmbedCandidates.addAll(parents);
        }
        allEmbedCandidates.addAll(children);

        for (AiKnowledgeSegmentDO segmentDO : allEmbedCandidates) {
            AiKnowledgeChunkBO source = findChunkBO(chunkBOs, segmentDO);
            if (source != null && !source.isEmbedEnabled()) {
                continue;
            }
            vectorSupport.write(vectorStoreClient, segmentDO, documentDO, parseResult);
        }

        knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                AiKnowledgeDocumentIngestStatusEnum.SUCCESS.getStatus(), null);
        log.info("[persistStructuredChunks][documentId={} parents={} children={} engine={}]",
                documentId, parents.size(), children.size(), parseResult.getEngine().getCode());
    }

    private static AiKnowledgeChunkBO findChunkBO(List<AiKnowledgeChunkBO> chunkBOs, AiKnowledgeSegmentDO segmentDO) {
        return chunkBOs.stream()
                .filter(chunk -> Objects.equals(chunk.getContent(), segmentDO.getContent()))
                .filter(chunk -> Objects.equals(chunk.getChunkLevel().getLevel(), segmentDO.getChunkLevel()))
                .findFirst()
                .orElse(null);
    }

    private static AiKnowledgeSegmentDO toSegmentDO(Long knowledgeId, Long documentId, AiKnowledgeChunkBO chunkBO) {
        String content = chunkBO.getContent();
        String embedText = StrUtil.blankToDefault(chunkBO.getEmbedText(), content);
        return new AiKnowledgeSegmentDO()
                .setKnowledgeId(knowledgeId)
                .setDocumentId(documentId)
                .setChunkLevel(chunkBO.getChunkLevel().getLevel())
                .setBlockType(chunkBO.getBlockType().getCode())
                .setHeadingPath(chunkBO.getHeadingPath())
                .setPageStart(chunkBO.getPageStart())
                .setPageEnd(chunkBO.getPageEnd())
                .setContent(content)
                .setEmbedText(embedText)
                .setSparseText(SparseTextNormalizer.normalize(embedText))
                .setContentLength(content.length())
                .setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY)
                .setTokens(AiTokenCounter.estimate(embedText))
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    public void createKnowledgeSegmentBySplitContent(Long documentId, String content) {
        AiKnowledgeDocumentDO documentDO = knowledgeDocumentService.validateKnowledgeDocumentExists(documentId);
        knowledgeService.validateKnowledgeExists(documentDO.getKnowledgeId());
        AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(documentDO.getKnowledgeId());

        List<String> textSegments = splitSupport.splitByStrategy(content, documentDO.getSegmentMaxTokens(),
                AiDocumentSplitStrategyEnum.AUTO, documentDO.getUrl());

        List<AiKnowledgeSegmentDO> segmentDOs = convertList(textSegments, segmentText -> {
            if (StrUtil.isEmpty(segmentText)) {
                return null;
            }
            return new AiKnowledgeSegmentDO().setKnowledgeId(documentDO.getKnowledgeId()).setDocumentId(documentId)
                    .setContent(segmentText).setEmbedText(segmentText)
                    .setSparseText(SparseTextNormalizer.normalize(segmentText))
                    .setContentLength(segmentText.length())
                    .setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY)
                    .setTokens(AiTokenCounter.estimate(segmentText))
                    .setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        segmentDOs = CollUtil.removeNull(segmentDOs);
        if (CollUtil.isNotEmpty(segmentDOs)) {
            segmentMapper.insertBatch(segmentDOs);
            knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                    AiKnowledgeDocumentIngestStatusEnum.EMBEDDING.getStatus(), null);
            int segmentIndex = 0;
            for (String segmentText : textSegments) {
                if (StrUtil.isEmpty(segmentText)) {
                    continue;
                }
                AiKnowledgeSegmentDO segmentDO = segmentDOs.get(segmentIndex++);
                vectorSupport.write(vectorStoreClient, segmentDO, segmentText);
            }
        }
        knowledgeDocumentService.updateKnowledgeDocumentIngestStatus(documentId,
                AiKnowledgeDocumentIngestStatusEnum.SUCCESS.getStatus(), null);
    }

    public void updateKnowledgeSegment(AiKnowledgeSegmentSaveReqVO reqVO) {
        AiKnowledgeSegmentDO oldSegment = validateKnowledgeSegmentExists(reqVO.getId());

        AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(oldSegment.getKnowledgeId());
        vectorSupport.delete(vectorStoreClient, oldSegment);

        AiKnowledgeSegmentDO newSegment = BeanUtils.toBean(reqVO, AiKnowledgeSegmentDO.class);
        segmentMapper.updateById(newSegment);
        if (CommonStatusEnum.isEnable(oldSegment.getStatus())) {
            newSegment.setKnowledgeId(oldSegment.getKnowledgeId()).setDocumentId(oldSegment.getDocumentId());
            vectorSupport.write(vectorStoreClient, newSegment, newSegment.getContent());
        }
    }

    public void deleteKnowledgeSegment(Long id) {
        AiKnowledgeSegmentDO segment = validateKnowledgeSegmentExists(id);

        AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(segment.getKnowledgeId());
        vectorSupport.delete(vectorStoreClient, segment);

        segmentMapper.deleteById(id);
    }

    public void deleteKnowledgeSegmentByDocumentId(Long documentId) {
        List<AiKnowledgeSegmentDO> segments = segmentMapper.selectListByDocumentId(documentId);
        if (CollUtil.isEmpty(segments)) {
            return;
        }

        List<String> vectorIds = listEmbeddedVectorIds(segments);
        if (CollUtil.isNotEmpty(vectorIds)) {
            AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(segments.get(0).getKnowledgeId());
            vectorSupport.safeDelete(vectorStoreClient, vectorIds);
        }

        segmentMapper.deleteByIds(convertList(segments, AiKnowledgeSegmentDO::getId));
    }

    public void updateKnowledgeSegmentStatus(AiKnowledgeSegmentUpdateStatusReqVO reqVO) {
        AiKnowledgeSegmentDO segment = validateKnowledgeSegmentExists(reqVO.getId());

        AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(segment.getKnowledgeId());

        segmentMapper.updateById(new AiKnowledgeSegmentDO().setId(reqVO.getId()).setStatus(reqVO.getStatus()));

        if (CommonStatusEnum.isEnable(reqVO.getStatus())) {
            vectorSupport.write(vectorStoreClient, segment, segment.getContent());
        } else {
            vectorSupport.delete(vectorStoreClient, segment);
        }
    }

    public void reindexKnowledgeSegmentByKnowledgeId(Long knowledgeId) {
        AiKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(knowledgeId);
        AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(knowledge);

        List<AiKnowledgeSegmentDO> segments = segmentMapper.selectListByKnowledgeIdAndStatus(
                knowledgeId, CommonStatusEnum.ENABLE.getStatus());
        if (CollUtil.isEmpty(segments)) {
            return;
        }
        for (AiKnowledgeSegmentDO segment : segments) {
            if (!vectorSupport.shouldEmbed(segment)) {
                continue;
            }
            vectorSupport.delete(vectorStoreClient, segment);
            vectorSupport.write(vectorStoreClient, segment, resolveEmbedContent(segment));
        }
        log.info("[reindexKnowledgeSegmentByKnowledgeId][知识库({}) 重新索引完成，共处理 {} 个段落]",
                knowledgeId, segments.size());
    }

    public void repairSegmentVector(Long segmentId) {
        AiKnowledgeSegmentDO segment = validateKnowledgeSegmentExists(segmentId);
        if (!CommonStatusEnum.isEnable(segment.getStatus())) {
            return;
        }
        AiVectorStoreClient vectorStoreClient = vectorSupport.getVectorStoreClient(segment.getKnowledgeId());
        if (StrUtil.isNotBlank(segment.getVectorId())
                && !AiKnowledgeSegmentDO.VECTOR_ID_EMPTY.equals(segment.getVectorId())) {
            vectorSupport.safeDelete(vectorStoreClient, List.of(segment.getVectorId()));
            segmentMapper.updateById(new AiKnowledgeSegmentDO()
                    .setId(segment.getId())
                    .setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY));
        }
        vectorSupport.write(vectorStoreClient, segment, resolveEmbedContent(segment));
    }

    public void repairSegmentSparseText(Long segmentId) {
        AiKnowledgeSegmentDO segment = validateKnowledgeSegmentExists(segmentId);
        String source = StrUtil.blankToDefault(segment.getEmbedText(), segment.getContent());
        if (StrUtil.isBlank(source)) {
            return;
        }
        String sparseText = SparseTextNormalizer.normalize(source);
        if (StrUtil.equals(sparseText, segment.getSparseText())) {
            return;
        }
        segmentMapper.updateById(new AiKnowledgeSegmentDO()
                .setId(segment.getId())
                .setSparseText(sparseText));
    }

    public List<AiKnowledgeSegmentDO> previewSplitContent(String url, Integer segmentMaxTokens) {
        String content = knowledgeDocumentService.readUrl(url);
        AiDocumentSplitStrategyEnum strategy = splitSupport.detectStrategy(content, url);
        List<String> textSegments = splitSupport.splitByStrategy(content, segmentMaxTokens, strategy, url);
        return convertList(textSegments, segmentText -> {
            if (StrUtil.isEmpty(segmentText)) {
                return null;
            }
            return new AiKnowledgeSegmentDO()
                    .setContent(segmentText)
                    .setContentLength(segmentText.length())
                    .setTokens(AiTokenCounter.estimate(segmentText));
        });
    }

    public Long createKnowledgeSegment(AiKnowledgeSegmentSaveReqVO createReqVO) {
        AiKnowledgeDocumentDO document = knowledgeDocumentService
                .validateKnowledgeDocumentExists(createReqVO.getDocumentId());
        AiKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(document.getKnowledgeId());
        Integer tokens = AiTokenCounter.estimate(createReqVO.getContent());
        if (tokens > document.getSegmentMaxTokens()) {
            throw exception(KNOWLEDGE_SEGMENT_CONTENT_TOO_LONG, tokens, document.getSegmentMaxTokens());
        }
        AiKnowledgeSegmentDO segment = BeanUtils.toBean(createReqVO, AiKnowledgeSegmentDO.class)
                .setKnowledgeId(knowledge.getId()).setDocumentId(document.getId())
                .setContentLength(createReqVO.getContent().length()).setTokens(tokens)
                .setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY)
                .setRetrievalCount(0).setStatus(CommonStatusEnum.ENABLE.getStatus());
        segmentMapper.insert(segment);
        vectorSupport.write(vectorSupport.getVectorStoreClient(knowledge), segment, segment.getContent());
        return segment.getId();
    }

    private AiKnowledgeSegmentDO validateKnowledgeSegmentExists(Long id) {
        AiKnowledgeSegmentDO knowledgeSegment = segmentMapper.selectById(id);
        if (knowledgeSegment == null) {
            throw exception(KNOWLEDGE_SEGMENT_NOT_EXISTS);
        }
        return knowledgeSegment;
    }

}
