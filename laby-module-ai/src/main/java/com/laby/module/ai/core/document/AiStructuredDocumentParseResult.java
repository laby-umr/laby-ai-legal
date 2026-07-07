package com.laby.module.ai.core.document;

import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文档解析结果（知识库入库入口）
 */
@Data
@Accessors(chain = true)
public class AiStructuredDocumentParseResult {

    /** Canonical Markdown / 纯文本（落库 content 字段） */
    private String markdown;

    private AiStructuredDocument structuredDocument = new AiStructuredDocument();

    /** 实际使用的解析引擎 */
    private AiDocumentParseEngineEnum engine = AiDocumentParseEngineEnum.TIKA;

    /** 解析质量 */
    private AiDocumentParseQualityEnum quality = AiDocumentParseQualityEnum.LOW;

    /** 是否经过引擎降级（MinerU/Docling 失败 → Tika） */
    private boolean degraded;

    public boolean supportsStructuredChunking() {
        return quality != AiDocumentParseQualityEnum.LOW
                && structuredDocument != null
                && structuredDocument.hasStructuredElements();
    }

}
