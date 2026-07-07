package com.laby.module.ai.framework.document;

import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识库文档解析配置（MinerU / Docling / Tika）
 */
@Data
@ConfigurationProperties(prefix = "laby.ai.document-parse")
public class DocumentParseProperties {

    /** 是否启用结构化解析管线（关闭时走原 Tika 纯文本） */
    private boolean enabled = true;

    /** 默认引擎：auto | mineru | docling | tika */
    private String defaultEngine = AiDocumentParseEngineEnum.AUTO.getCode();

    /** 扩展名 → 引擎 code 覆盖（如 html→html, csv→spreadsheet） */
    private Map<String, String> routeOverrides = new HashMap<>();

    private EngineConfig mineru = new EngineConfig()
            .setEnabled(false)
            .setBaseUrl("http://127.0.0.1:8000")
            .setParsePath("/api/v1/parse")
            .setTimeoutMs(300_000);

    private EngineConfig docling = new EngineConfig()
            .setEnabled(false)
            .setBaseUrl("http://127.0.0.1:8001")
            .setParsePath("/api/v1/parse")
            .setTimeoutMs(120_000);

    private StructuredChunkConfig structuredChunk = new StructuredChunkConfig();

    @Data
    public static class EngineConfig {

        private boolean enabled;
        private String baseUrl;
        private String parsePath;
        private int timeoutMs = 120_000;

    }

    @Data
    public static class StructuredChunkConfig {

        private int childMaxTokens = 512;
        private int childOverlapTokens = 80;
        private int parentMaxTokens = 2000;
        private boolean embedParent = false;

        /** 表格行级索引（table_row） */
        private boolean tableRowIndexEnabled = true;

        /** 表格规则摘要索引（table_summary） */
        private boolean tableSummaryEnabled = true;

        /** 检索时 Parent / 整表上下文回填 */
        private boolean parentBackfillEnabled = true;

    }

}
