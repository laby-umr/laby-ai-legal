package com.laby.module.ai.framework.document;

import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;

/**
 * MinerU HTTP 解析客户端
 */
public class HttpMinerUDocumentParseClient extends AbstractHttpDocumentParseClient {

    public HttpMinerUDocumentParseClient(DocumentParseProperties properties) {
        super(properties);
    }

    @Override
    public AiDocumentParseEngineEnum engine() {
        return AiDocumentParseEngineEnum.MINERU;
    }

    @Override
    protected DocumentParseProperties.EngineConfig engineConfig() {
        return properties.getMineru();
    }

    @Override
    protected AiDocumentParseEngineEnum engineEnum() {
        return AiDocumentParseEngineEnum.MINERU;
    }

    @Override
    protected AiDocumentParseQualityEnum defaultQuality() {
        return AiDocumentParseQualityEnum.HIGH;
    }

}
