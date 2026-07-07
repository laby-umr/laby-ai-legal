package com.laby.module.ai.framework.document;

import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;

/**
 * 知识库文档解析门面
 */
public class DocumentParseService {

    private final DocumentParseRouter router;

    public DocumentParseService(DocumentParseRouter router) {
        this.router = router;
    }

    public AiStructuredDocumentParseResult parse(byte[] bytes, String fileName) {
        return router.parse(bytes, fileName);
    }

}
