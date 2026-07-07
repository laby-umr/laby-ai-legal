package com.laby.module.ai.framework.document;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 文档解析引擎路由（MinerU / Docling / 专用解析 / Tika 降级）
 */
@Slf4j
public class DocumentParseRouter {

    private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");
    private static final Set<String> OFFICE_EXTENSIONS = Set.of("docx", "doc", "pptx");
    private static final Set<String> SPREADSHEET_EXTENSIONS = Set.of("xlsx", "xls", "csv");
    private static final Set<String> HTML_EXTENSIONS = Set.of("html", "htm");
    private static final Set<String> EMAIL_EXTENSIONS = Set.of("eml", "msg");

    private final DocumentParseProperties properties;
    private final TikaDocumentParseClient tikaClient;
    private final HttpMinerUDocumentParseClient mineruClient;
    private final HttpDoclingDocumentParseClient doclingClient;
    private final HtmlStructuredDocumentParseClient htmlClient;
    private final SpreadsheetDocumentParseClient spreadsheetClient;
    private final EmailDocumentParseClient emailClient;

    public DocumentParseRouter(DocumentParseProperties properties,
                               TikaDocumentParseClient tikaClient,
                               HttpMinerUDocumentParseClient mineruClient,
                               HttpDoclingDocumentParseClient doclingClient,
                               HtmlStructuredDocumentParseClient htmlClient,
                               SpreadsheetDocumentParseClient spreadsheetClient,
                               EmailDocumentParseClient emailClient) {
        this.properties = properties;
        this.tikaClient = tikaClient;
        this.mineruClient = mineruClient;
        this.doclingClient = doclingClient;
        this.htmlClient = htmlClient;
        this.spreadsheetClient = spreadsheetClient;
        this.emailClient = emailClient;
    }

    public AiStructuredDocumentParseResult parse(byte[] bytes, String fileName) {
        if (!properties.isEnabled()) {
            return tikaClient.parse(bytes, fileName);
        }
        DocumentParseClient primary = resolveClient(fileName);
        if (primary.engine() == AiDocumentParseEngineEnum.TIKA) {
            return tikaClient.parse(bytes, fileName);
        }
        try {
            AiStructuredDocumentParseResult result = primary.parse(bytes, fileName);
            if (StrUtil.isBlank(result.getMarkdown())) {
                log.warn("[parse][fileName={} engine={}] 解析结果为空，降级 Tika", fileName, primary.engine().getCode());
                return degrade(tikaClient.parse(bytes, fileName), primary.engine());
            }
            return result;
        } catch (Exception ex) {
            log.warn("[parse][fileName={} engine={}] 解析失败，降级 Tika: {}",
                    fileName, primary.engine().getCode(), ex.getMessage());
            return degrade(tikaClient.parse(bytes, fileName), primary.engine());
        }
    }

    DocumentParseClient resolveClient(String fileName) {
        String ext = extension(fileName);
        Map<String, String> overrides = properties.getRouteOverrides();
        if (StrUtil.isNotBlank(ext) && overrides != null && overrides.containsKey(ext)) {
            return clientOf(AiDocumentParseEngineEnum.valueOfCode(overrides.get(ext)));
        }
        AiDocumentParseEngineEnum configured = AiDocumentParseEngineEnum.valueOfCode(properties.getDefaultEngine());
        if (configured != AiDocumentParseEngineEnum.AUTO) {
            return clientOf(configured);
        }
        if (HTML_EXTENSIONS.contains(ext)) {
            return htmlClient;
        }
        if (SPREADSHEET_EXTENSIONS.contains(ext)) {
            return spreadsheetClient;
        }
        if (EMAIL_EXTENSIONS.contains(ext)) {
            return emailClient;
        }
        if (PDF_EXTENSIONS.contains(ext) && mineruClient.isAvailable()) {
            return mineruClient;
        }
        if ((PDF_EXTENSIONS.contains(ext) || OFFICE_EXTENSIONS.contains(ext)) && doclingClient.isAvailable()) {
            return doclingClient;
        }
        return tikaClient;
    }

    private DocumentParseClient clientOf(AiDocumentParseEngineEnum engine) {
        return switch (engine) {
            case MINERU -> mineruClient.isAvailable() ? mineruClient : tikaClient;
            case DOCLING -> doclingClient.isAvailable() ? doclingClient : tikaClient;
            case HTML -> htmlClient;
            case SPREADSHEET -> spreadsheetClient;
            case EMAIL -> emailClient;
            case TIKA -> tikaClient;
            case AUTO -> tikaClient;
        };
    }

    private static AiStructuredDocumentParseResult degrade(AiStructuredDocumentParseResult tikaResult,
                                                           AiDocumentParseEngineEnum failedEngine) {
        return tikaResult.setDegraded(true)
                .setEngine(AiDocumentParseEngineEnum.TIKA)
                .setQuality(AiDocumentParseQualityEnum.LOW);
    }

    private static String extension(String fileName) {
        String ext = FileNameUtil.extName(fileName);
        return StrUtil.isBlank(ext) ? "" : ext.toLowerCase(Locale.ROOT);
    }

}
