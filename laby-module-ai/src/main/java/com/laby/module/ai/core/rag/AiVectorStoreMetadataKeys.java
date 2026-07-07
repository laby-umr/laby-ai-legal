package com.laby.module.ai.core.rag;

import java.util.HashMap;
import java.util.Map;

/**
 * Qdrant payload 元数据键（知识库 segment / 合同段落向量共用）
 */
public final class AiVectorStoreMetadataKeys {



    public static final String KNOWLEDGE_ID = "knowledgeId";

    public static final String DOCUMENT_ID = "documentId";

    public static final String SEGMENT_ID = "segmentId";

    public static final String EMBEDDING_MODEL_ID = "embeddingModelId";

    public static final String INDEXED_AT = "indexedAt";

    public static final String TENANT_ID = "tenantId";



    /** 结构化分片：块类型，见 {@link com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum} */

    public static final String BLOCK_TYPE = "blockType";

    /** 结构化分片：层级 0=child 1=parent */

    public static final String CHUNK_LEVEL = "chunkLevel";

    public static final String PARENT_SEGMENT_ID = "parentSegmentId";

    public static final String PAGE_START = "pageStart";

    public static final String HEADING_PATH = "headingPath";

    /** 解析引擎 code，见 {@link com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum} */

    public static final String PARSE_ENGINE = "parseEngine";

    /** 解析质量 code，见 {@link com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum} */

    public static final String PARSE_QUALITY = "parseQuality";

    /** 文档类型 code，见 {@link com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentTypeEnum} */

    public static final String DOCUMENT_TYPE = "documentType";

    /** 邮件主题 */

    public static final String EMAIL_SUBJECT = "emailSubject";

    /** Excel sheet 名称 */

    public static final String SHEET_NAME = "sheetName";

    /** PPT 页码（从 1 起） */

    public static final String SLIDE_INDEX = "slideIndex";



    private AiVectorStoreMetadataKeys() {

    }

    /**
     * 知识库向量检索过滤条件。
     * <p>
     * 仅按 {@link #KNOWLEDGE_ID} 过滤：knowledgeId 全局唯一，且历史向量可能未写入 tenantId payload，
     * 若同时过滤 tenantId 会导致 Dense 恒为 0。
     */
    public static Map<String, String> knowledgeSearchFilter(Long knowledgeId) {
        Map<String, String> metadata = new HashMap<>(1);
        metadata.put(KNOWLEDGE_ID, knowledgeId.toString());
        return metadata;
    }

}

