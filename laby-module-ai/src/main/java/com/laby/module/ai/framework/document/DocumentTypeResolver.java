package com.laby.module.ai.framework.document;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentTypeEnum;

/**
 * 根据扩展名与解析引擎推断文档类型
 */
public final class DocumentTypeResolver {

    private DocumentTypeResolver() {
    }

    public static AiKnowledgeDocumentTypeEnum resolve(String fileName, AiStructuredDocumentParseResult parseResult) {
        AiKnowledgeDocumentTypeEnum byExtension = AiKnowledgeDocumentTypeEnum.fromFileName(fileName);
        if (byExtension != AiKnowledgeDocumentTypeEnum.UNKNOWN) {
            return byExtension;
        }
        if (parseResult != null && parseResult.getEngine() != null) {
            return switch (parseResult.getEngine()) {
                case HTML -> AiKnowledgeDocumentTypeEnum.HTML;
                case SPREADSHEET -> AiKnowledgeDocumentTypeEnum.SPREADSHEET;
                case EMAIL -> AiKnowledgeDocumentTypeEnum.EMAIL;
                default -> AiKnowledgeDocumentTypeEnum.UNKNOWN;
            };
        }
        return AiKnowledgeDocumentTypeEnum.UNKNOWN;
    }

    public static AiKnowledgeDocumentTypeEnum resolveFromUrl(String url, AiStructuredDocumentParseResult parseResult) {
        if (StrUtil.isBlank(url)) {
            return resolve(null, parseResult);
        }
        String fileName = FileNameUtil.getName(StrUtil.subBefore(url, "?", false));
        return resolve(fileName, parseResult);
    }

}
