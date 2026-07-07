package com.laby.module.ai.framework.document;

import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;

/**
 * 文档解析客户端
 */
public interface DocumentParseClient {

    AiDocumentParseEngineEnum engine();

    boolean isAvailable();

    AiStructuredDocumentParseResult parse(byte[] bytes, String fileName);

}
