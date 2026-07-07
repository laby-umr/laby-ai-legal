package com.laby.module.ai.framework.document;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentTypeEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeChunkBO;
import com.laby.module.ai.service.knowledge.splitter.HierarchicalKnowledgeChunker;
import com.laby.module.ai.service.knowledge.splitter.MarkdownQaSplitter;
import com.laby.module.ai.service.knowledge.splitter.SemanticTextSplitter;
import com.laby.module.ai.service.knowledge.splitter.SpreadsheetTableChunker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 按文档类型路由分片策略
 */
public class DocumentTypeChunkRouter {

    private static final Pattern QA_HEADING_PATTERN = Pattern.compile("^##\\s+.+$", Pattern.MULTILINE);

    private final DocumentParseProperties properties;

    public DocumentTypeChunkRouter(DocumentParseProperties properties) {
        this.properties = properties;
    }

    public List<AiKnowledgeChunkBO> chunk(String documentTypeCode,
                                          AiStructuredDocumentParseResult parseResult,
                                          Integer segmentMaxTokens) {
        AiKnowledgeDocumentTypeEnum documentType = AiKnowledgeDocumentTypeEnum.valueOfCode(documentTypeCode);
        return switch (documentType) {
            case SPREADSHEET, CSV -> new SpreadsheetTableChunker(properties).chunk(parseResult);
            case MARKDOWN -> chunkMarkdown(parseResult, segmentMaxTokens);
            case EMAIL -> chunkEmail(parseResult, segmentMaxTokens);
            default -> {
                if (parseResult != null && parseResult.supportsStructuredChunking()) {
                    yield new HierarchicalKnowledgeChunker(properties).chunk(parseResult);
                }
                yield chunkSemantic(parseResult != null ? parseResult.getMarkdown() : "", segmentMaxTokens);
            }
        };
    }

    private List<AiKnowledgeChunkBO> chunkMarkdown(AiStructuredDocumentParseResult parseResult,
                                                   Integer segmentMaxTokens) {
        if (parseResult != null && parseResult.supportsStructuredChunking()) {
            return new HierarchicalKnowledgeChunker(properties).chunk(parseResult);
        }
        String markdown = parseResult != null ? parseResult.getMarkdown() : "";
        int maxTokens = resolveMaxTokens(segmentMaxTokens);
        if (StrUtil.isNotBlank(markdown) && QA_HEADING_PATTERN.matcher(markdown).find()) {
            return toChunkBOs(new MarkdownQaSplitter(maxTokens).split(markdown));
        }
        return chunkSemantic(markdown, segmentMaxTokens);
    }

    private List<AiKnowledgeChunkBO> chunkEmail(AiStructuredDocumentParseResult parseResult,
                                                Integer segmentMaxTokens) {
        if (parseResult != null && parseResult.supportsStructuredChunking()) {
            return new HierarchicalKnowledgeChunker(properties).chunk(parseResult);
        }
        return chunkSemantic(parseResult != null ? parseResult.getMarkdown() : "", segmentMaxTokens);
    }

    private List<AiKnowledgeChunkBO> chunkSemantic(String content, Integer segmentMaxTokens) {
        if (StrUtil.isBlank(content)) {
            return List.of();
        }
        DocumentParseProperties.StructuredChunkConfig config = properties.getStructuredChunk();
        int maxTokens = resolveMaxTokens(segmentMaxTokens);
        SemanticTextSplitter splitter = new SemanticTextSplitter(maxTokens, config.getChildOverlapTokens());
        return toChunkBOs(splitter.split(content));
    }

    private List<AiKnowledgeChunkBO> toChunkBOs(List<String> segments) {
        List<AiKnowledgeChunkBO> chunks = new ArrayList<>();
        for (String segment : segments) {
            if (StrUtil.isBlank(segment)) {
                continue;
            }
            chunks.add(new AiKnowledgeChunkBO()
                    .setContent(segment)
                    .setEmbedText(segment)
                    .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TEXT)
                    .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                    .setEmbedEnabled(true));
        }
        return chunks;
    }

    private int resolveMaxTokens(Integer segmentMaxTokens) {
        if (segmentMaxTokens != null && segmentMaxTokens > 0) {
            return segmentMaxTokens;
        }
        return properties.getStructuredChunk().getChildMaxTokens();
    }

}
