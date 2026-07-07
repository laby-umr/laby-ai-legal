package com.laby.module.ai.service.knowledge.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.core.rag.AiVectorDocument;
import com.laby.module.ai.core.rag.AiVectorStoreClient;
import com.laby.module.ai.core.rag.AiVectorStoreMetadataKeys;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.dal.mysql.knowledge.AiKnowledgeSegmentMapper;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import com.laby.module.ai.framework.document.DocumentParseProperties;
import com.laby.module.ai.service.knowledge.AiKnowledgeDocumentService;
import com.laby.module.ai.service.knowledge.AiKnowledgeService;
import com.laby.module.ai.service.model.AiModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库分段向量写入、删除与元数据构建。
 */
@Slf4j
@Component
public class AiKnowledgeSegmentVectorSupport {

    @Resource
    private AiKnowledgeSegmentMapper segmentMapper;
    @Resource
    private AiKnowledgeService knowledgeService;
    @Resource
    @Lazy // 延迟加载，避免与 Segment/Document 循环依赖
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private AiModelService modelService;
    @Resource
    private DocumentParseProperties documentParseProperties;

    public AiVectorStoreClient getVectorStoreClient(AiKnowledgeDO knowledge) {
        return modelService.getVectorStoreClient(knowledge.getEmbeddingModelId());
    }

    public AiVectorStoreClient getVectorStoreClient(Long knowledgeId) {
        return getVectorStoreClient(knowledgeService.validateKnowledgeExists(knowledgeId));
    }

    public void write(AiVectorStoreClient vectorStoreClient, AiKnowledgeSegmentDO segmentDO, String content) {
        AiKnowledgeDocumentDO documentDO = knowledgeDocumentService.getKnowledgeDocument(segmentDO.getDocumentId());
        write(vectorStoreClient, segmentDO, documentDO, null, content);
    }

    public void write(AiVectorStoreClient vectorStoreClient,
                      AiKnowledgeSegmentDO segmentDO,
                      AiKnowledgeDocumentDO documentDO,
                      AiStructuredDocumentParseResult parseResult) {
        write(vectorStoreClient, segmentDO, documentDO, parseResult, resolveEmbedContent(segmentDO));
    }

    public void write(AiVectorStoreClient vectorStoreClient,
                      AiKnowledgeSegmentDO segmentDO,
                      AiKnowledgeDocumentDO documentDO,
                      AiStructuredDocumentParseResult parseResult,
                      String content) {
        AiKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(segmentDO.getKnowledgeId());
        Map<String, String> metadata = buildMetadata(segmentDO, documentDO, parseResult, knowledge);
        AiVectorDocument doc = new AiVectorDocument()
                .setContent(content)
                .setMetadata(metadata);
        vectorStoreClient.add(List.of(doc));
        segmentMapper.updateById(new AiKnowledgeSegmentDO().setId(segmentDO.getId()).setVectorId(doc.getId()));
    }

    public void delete(AiVectorStoreClient vectorStoreClient, AiKnowledgeSegmentDO segmentDO) {
        if (StrUtil.isEmpty(segmentDO.getVectorId())) {
            return;
        }
        segmentMapper.updateById(new AiKnowledgeSegmentDO().setId(segmentDO.getId())
                .setVectorId(AiKnowledgeSegmentDO.VECTOR_ID_EMPTY));
        safeDelete(vectorStoreClient, List.of(segmentDO.getVectorId()));
    }

    public void safeDelete(AiVectorStoreClient vectorStoreClient, List<String> vectorIds) {
        if (CollUtil.isEmpty(vectorIds)) {
            return;
        }
        try {
            vectorStoreClient.delete(vectorIds);
        } catch (Exception ex) {
            log.warn("[safeDelete][Qdrant 向量删除失败，已跳过 vectorIds={}]", vectorIds, ex);
        }
    }

    public boolean shouldEmbed(AiKnowledgeSegmentDO segment) {
        if (segment.getChunkLevel() == null) {
            return true;
        }
        if (AiKnowledgeSegmentChunkLevelEnum.PARENT.getLevel().equals(segment.getChunkLevel())) {
            return documentParseProperties.getStructuredChunk().isEmbedParent();
        }
        return true;
    }

    public static String resolveEmbedContent(AiKnowledgeSegmentDO segment) {
        return StrUtil.blankToDefault(segment.getEmbedText(), segment.getContent());
    }

    public static List<String> listEmbeddedVectorIds(List<AiKnowledgeSegmentDO> segments) {
        return segments.stream()
                .map(AiKnowledgeSegmentDO::getVectorId)
                .filter(StrUtil::isNotBlank)
                .filter(vectorId -> !AiKnowledgeSegmentDO.VECTOR_ID_EMPTY.equals(vectorId))
                .toList();
    }

    private Map<String, String> buildMetadata(AiKnowledgeSegmentDO segmentDO,
                                              AiKnowledgeDocumentDO documentDO,
                                              AiStructuredDocumentParseResult parseResult,
                                              AiKnowledgeDO knowledge) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(AiVectorStoreMetadataKeys.KNOWLEDGE_ID, segmentDO.getKnowledgeId().toString());
        metadata.put(AiVectorStoreMetadataKeys.DOCUMENT_ID, segmentDO.getDocumentId().toString());
        metadata.put(AiVectorStoreMetadataKeys.SEGMENT_ID, segmentDO.getId().toString());
        if (knowledge.getEmbeddingModelId() != null) {
            metadata.put(AiVectorStoreMetadataKeys.EMBEDDING_MODEL_ID, knowledge.getEmbeddingModelId().toString());
        }
        if (knowledge.getTenantId() != null) {
            metadata.put(AiVectorStoreMetadataKeys.TENANT_ID, knowledge.getTenantId().toString());
        }
        metadata.put(AiVectorStoreMetadataKeys.INDEXED_AT, LocalDateTime.now().toString());
        if (StrUtil.isNotBlank(segmentDO.getBlockType())) {
            metadata.put(AiVectorStoreMetadataKeys.BLOCK_TYPE, segmentDO.getBlockType());
        }
        if (segmentDO.getChunkLevel() != null) {
            metadata.put(AiVectorStoreMetadataKeys.CHUNK_LEVEL, segmentDO.getChunkLevel().toString());
        }
        if (segmentDO.getParentId() != null) {
            metadata.put(AiVectorStoreMetadataKeys.PARENT_SEGMENT_ID, segmentDO.getParentId().toString());
        }
        if (segmentDO.getPageStart() != null) {
            metadata.put(AiVectorStoreMetadataKeys.PAGE_START, segmentDO.getPageStart().toString());
        }
        if (StrUtil.isNotBlank(segmentDO.getHeadingPath())) {
            metadata.put(AiVectorStoreMetadataKeys.HEADING_PATH, segmentDO.getHeadingPath());
        }
        String parseEngine = documentDO != null ? documentDO.getParseEngine() : null;
        if (parseResult != null && parseResult.getEngine() != null) {
            parseEngine = parseResult.getEngine().getCode();
        }
        if (StrUtil.isNotBlank(parseEngine)) {
            metadata.put(AiVectorStoreMetadataKeys.PARSE_ENGINE, parseEngine);
        }
        String parseQuality = documentDO != null ? documentDO.getParseQuality() : null;
        if (parseResult != null && parseResult.getQuality() != null) {
            parseQuality = parseResult.getQuality().getCode();
        }
        if (StrUtil.isNotBlank(parseQuality)) {
            metadata.put(AiVectorStoreMetadataKeys.PARSE_QUALITY, parseQuality);
        }
        return metadata;
    }

}
