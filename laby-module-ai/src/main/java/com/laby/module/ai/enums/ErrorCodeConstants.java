package com.laby.module.ai.enums;

import com.laby.framework.common.exception.ErrorCode;

/**
 * AI 错误码枚举类
 * <p>
 * ai 系统，使用 1-040-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== API 密钥 1-040-000-000 ==========
    ErrorCode API_KEY_NOT_EXISTS = new ErrorCode(1_040_000_000, "API 密钥不存在");
    ErrorCode API_KEY_DISABLE = new ErrorCode(1_040_000_001, "API 密钥已禁用！");

    // ========== API 模型 1-040-001-000 ==========
    ErrorCode MODEL_NOT_EXISTS = new ErrorCode(1_040_001_000, "模型不存在!");
    ErrorCode MODEL_DISABLE = new ErrorCode(1_040_001_001, "模型({})已禁用!");
    ErrorCode MODEL_DEFAULT_NOT_EXISTS = new ErrorCode(1_040_001_002, "操作失败，找不到默认模型");
    ErrorCode MODEL_USE_TYPE_ERROR = new ErrorCode(1_040_001_003, "操作失败，该模型的模型类型不正确");

    // ========== API 聊天角色 1-040-002-000 ==========
    ErrorCode CHAT_ROLE_NOT_EXISTS = new ErrorCode(1_040_002_000, "聊天角色不存在");
    ErrorCode CHAT_ROLE_DISABLE = new ErrorCode(1_040_002_001, "聊天角色({})已禁用!");

    // ========== API 聊天会话 1-040-003-000 ==========
    ErrorCode CHAT_CONVERSATION_NOT_EXISTS = new ErrorCode(1_040_003_000, "对话不存在!");
    ErrorCode CHAT_CONVERSATION_MODEL_ERROR = new ErrorCode(1_040_003_001, "操作失败，该聊天模型的配置不完整");

    // ========== API 聊天消息 1-040-004-000 ==========
    ErrorCode CHAT_MESSAGE_NOT_EXIST = new ErrorCode(1_040_004_000, "消息不存在!");
    ErrorCode CHAT_STREAM_ERROR = new ErrorCode(1_040_004_001, "对话生成异常!");
    ErrorCode CHAT_STREAM_MODEL_AUTH_ERROR = new ErrorCode(1_040_004_002, "模型渠道鉴权失败，请检查 API Key / Base URL 配置");
    ErrorCode CHAT_CONVERSATION_STREAM_BUSY = new ErrorCode(1_040_004_003, "当前对话正在处理上一条消息，请稍后再试");

    // ========== API 绘画 1-040-005-000 ==========
    ErrorCode IMAGE_NOT_EXISTS = new ErrorCode(1_040_005_000, "图片不存在!");
    ErrorCode IMAGE_MIDJOURNEY_SUBMIT_FAIL = new ErrorCode(1_040_005_001, "Midjourney 提交失败!原因：{}");
    ErrorCode IMAGE_CUSTOM_ID_NOT_EXISTS = new ErrorCode(1_040_005_002, "Midjourney 按钮 customId 不存在! {}");

    // ========== API 音乐 1-040-006-000 ==========
    ErrorCode MUSIC_NOT_EXISTS = new ErrorCode(1_040_006_000, "音乐不存在!");

    // ========== API 写作 1-040-007-000 ==========
    ErrorCode WRITE_NOT_EXISTS = new ErrorCode(1_040_007_000, "作文不存在!");
    ErrorCode WRITE_STREAM_ERROR = new ErrorCode(1_040_007_001, "写作生成异常!");

    // ========== API 思维导图 1-040-008-000 ==========
    ErrorCode MIND_MAP_NOT_EXISTS = new ErrorCode(1_040_008_000, "思维导图不存在!");

    // ========== API 知识库 1-040-009-000 ==========
    ErrorCode KNOWLEDGE_NOT_EXISTS = new ErrorCode(1_040_009_000, "知识库不存在!");

    ErrorCode KNOWLEDGE_DOCUMENT_NOT_EXISTS = new ErrorCode(1_040_009_101, "文档不存在!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_EMPTY = new ErrorCode(1_040_009_102, "文档内容为空!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_DOWNLOAD_FAIL = new ErrorCode(1_040_009_103, "文件下载失败!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_READ_FAIL = new ErrorCode(1_040_009_104, "文档加载失败!");
    ErrorCode KNOWLEDGE_DOCUMENT_INGEST_RUNNING = new ErrorCode(1_040_009_105, "文档正在入库处理中，请稍后再试");
    ErrorCode KNOWLEDGE_DOCUMENT_INGEST_CONTENT_EMPTY = new ErrorCode(1_040_009_106, "文档内容为空，无法重新向量化");
    ErrorCode KNOWLEDGE_DOCUMENT_URL_DUPLICATE_RETRY = new ErrorCode(1_040_009_107, "文档「{}」已存在且未完成入库，请在文档列表使用「重新入库」");
    ErrorCode KNOWLEDGE_DOCUMENT_URL_EXISTS = new ErrorCode(1_040_009_108, "文档「{}」已入库完成，请先删除后再上传");
    ErrorCode KNOWLEDGE_DOCUMENT_URL_BATCH_DUPLICATE = new ErrorCode(1_040_009_109, "本次上传存在重复文件，请合并后再提交");
    ErrorCode KNOWLEDGE_DOCUMENT_PARSE_FAIL = new ErrorCode(1_040_009_110, "文档解析失败：{}");
    ErrorCode KNOWLEDGE_DOCUMENT_PARSE_ENGINE_UNAVAILABLE = new ErrorCode(1_040_009_111, "文档解析引擎({})不可用，已降级处理");
    ErrorCode KNOWLEDGE_RETRIEVAL_SPARSE_FAIL = new ErrorCode(1_040_009_112, "混合检索 Sparse 路失败，已降级 Dense");
    ErrorCode KNOWLEDGE_RETRIEVAL_MULTI_QUERY_TIMEOUT = new ErrorCode(1_040_009_113, "Multi-Query LLM 超时，已降级规则扩展");
    ErrorCode KNOWLEDGE_RETRIEVAL_RERANK_UNAVAILABLE = new ErrorCode(1_040_009_114, "Rerank 不可用，已降级向量分排序");
    ErrorCode KNOWLEDGE_RETRIEVAL_NO_ANSWER_GUARD = new ErrorCode(1_040_009_115, "召回分数低于阈值，触发无引用守卫");
    ErrorCode KNOWLEDGE_RETRIEVAL_DISABLED = new ErrorCode(1_040_009_116, "Universal RAG 检索未启用，请配置 laby.ai.knowledge-retrieval.enabled=true");

    ErrorCode KNOWLEDGE_SEGMENT_NOT_EXISTS = new ErrorCode(1_040_009_202, "段落不存在!");
    ErrorCode KNOWLEDGE_SEGMENT_CONTENT_TOO_LONG = new ErrorCode(1_040_009_203, "内容 Token 数为 {}，超过最大限制 {}");

    // ========== AI 工具 1-040-010-000 ==========
    ErrorCode TOOL_NOT_EXISTS = new ErrorCode(1_040_010_000, "工具不存在");
    ErrorCode TOOL_NAME_NOT_EXISTS = new ErrorCode(1_040_010_001, "工具({})找不到 Bean");

    // ========== AI 工作流 1-040-011-000 ==========
    ErrorCode WORKFLOW_NOT_EXISTS = new ErrorCode(1_040_011_000, "工作流不存在");
    ErrorCode WORKFLOW_CODE_EXISTS = new ErrorCode(1_040_011_001, "工作流标识已存在");

}
