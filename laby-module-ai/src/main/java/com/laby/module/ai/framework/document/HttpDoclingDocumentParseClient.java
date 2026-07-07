package com.laby.module.ai.framework.document;

import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;

/**
 * Docling HTTP 解析客户端
 */
public class HttpDoclingDocumentParseClient extends AbstractHttpDocumentParseClient {

    public HttpDoclingDocumentParseClient(DocumentParseProperties properties) {
        super(properties);
    }

    @Override
    public AiDocumentParseEngineEnum engine() {
        return AiDocumentParseEngineEnum.DOCLING;
    }

    @Override
    protected DocumentParseProperties.EngineConfig engineConfig() {
        return properties.getDocling();
    }

    @Override
    protected AiDocumentParseEngineEnum engineEnum() {
        return AiDocumentParseEngineEnum.DOCLING;
    }

    @Override
    protected AiDocumentParseQualityEnum defaultQuality() {
        return AiDocumentParseQualityEnum.STANDARD;
    }

}
