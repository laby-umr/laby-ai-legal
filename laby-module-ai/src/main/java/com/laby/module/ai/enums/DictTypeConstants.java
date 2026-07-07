package com.laby.module.ai.enums;

/**
 * AI 字典类型的枚举类
 *
 * @author xiaoxin
 */
public interface DictTypeConstants {

    // ========== AI Write ==========
    String AI_WRITE_FORMAT = "ai_write_format"; // 写作格式
    String AI_WRITE_LENGTH = "ai_write_length"; // 写作长度
    String AI_WRITE_LANGUAGE = "ai_write_language"; // 写作语言
    String AI_WRITE_TONE = "ai_write_tone"; // 写作语气

    // ========== AI Knowledge ==========
    String AI_KNOWLEDGE_DOCUMENT_INGEST_STATUS = "ai_knowledge_document_ingest_status"; // 知识库文档入库状态
    String AI_KNOWLEDGE_DOCUMENT_TYPE = "ai_knowledge_document_type"; // 知识库文档类型
    String AI_KNOWLEDGE_PARSE_ENGINE = "ai_knowledge_parse_engine"; // 知识库解析引擎
    String AI_KNOWLEDGE_SEGMENT_BLOCK_TYPE = "ai_knowledge_segment_block_type"; // 知识库分片块类型
    String AI_KNOWLEDGE_SEGMENT_CHUNK_LEVEL = "ai_knowledge_segment_chunk_level"; // 知识库分片层级

}
