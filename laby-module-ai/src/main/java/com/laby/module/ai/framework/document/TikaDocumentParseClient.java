package com.laby.module.ai.framework.document;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import java.io.ByteArrayInputStream;

/**
 * Apache Tika 纯文本解析（降级路径）
 */
public class TikaDocumentParseClient implements DocumentParseClient {

    private static final Tika TIKA = new Tika();

    @Override
    public AiDocumentParseEngineEnum engine() {
        return AiDocumentParseEngineEnum.TIKA;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiStructuredDocumentParseResult parse(byte[] bytes, String fileName) {
        try {
            Metadata metadata = new Metadata();
            if (StrUtil.isNotBlank(fileName)) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }
            String content = TIKA.parseToString(new ByteArrayInputStream(bytes), metadata);
            return new AiStructuredDocumentParseResult()
                    .setMarkdown(StrUtil.nullToEmpty(content))
                    .setEngine(AiDocumentParseEngineEnum.TIKA)
                    .setQuality(AiDocumentParseQualityEnum.LOW);
        } catch (Exception ex) {
            throw new IllegalStateException("Tika parse failed: " + ex.getMessage(), ex);
        }
    }

}
