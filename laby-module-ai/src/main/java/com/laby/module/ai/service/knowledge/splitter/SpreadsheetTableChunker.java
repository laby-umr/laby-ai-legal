package com.laby.module.ai.service.knowledge.splitter;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocument;
import com.laby.module.ai.core.document.AiStructuredDocumentElement;
import com.laby.module.ai.core.document.AiStructuredDocumentElementTypeEnum;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import com.laby.module.ai.framework.document.DocumentParseProperties;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeChunkBO;

import java.util.ArrayList;
import java.util.List;

/**
 * 电子表格分片器（整表 + 行级 + 摘要，对齐 PDF 表格三索引）
 */
public class SpreadsheetTableChunker {

    private final DocumentParseProperties.StructuredChunkConfig config;

    public SpreadsheetTableChunker(DocumentParseProperties properties) {
        this.config = properties.getStructuredChunk();
    }

    public List<AiKnowledgeChunkBO> chunk(AiStructuredDocumentParseResult parseResult) {
        if (parseResult == null || parseResult.getStructuredDocument() == null) {
            return List.of();
        }
        AiStructuredDocument document = parseResult.getStructuredDocument();
        List<AiKnowledgeChunkBO> chunks = new ArrayList<>();
        for (AiStructuredDocumentElement element : document.getElements()) {
            AiStructuredDocumentElementTypeEnum type =
                    AiStructuredDocumentElementTypeEnum.valueOfCode(element.getType());
            if (type != AiStructuredDocumentElementTypeEnum.TABLE) {
                continue;
            }
            chunks.addAll(buildTableChunks(element));
        }
        return chunks.stream().filter(chunk -> StrUtil.isNotBlank(chunk.getContent())).toList();
    }

    private List<AiKnowledgeChunkBO> buildTableChunks(AiStructuredDocumentElement element) {
        String tableMarkdown = StrUtil.blankToDefault(element.getMarkdown(), element.getText());
        String sheetName = StrUtil.blankToDefault(element.getCaption(), "Sheet");
        String wholeContent = sheetName + "\n" + tableMarkdown;
        String headingPath = "Sheet:" + sheetName;

        List<AiKnowledgeChunkBO> chunks = new ArrayList<>();
        chunks.add(new AiKnowledgeChunkBO()
                .setContent(wholeContent.trim())
                .setEmbedText(buildEmbedText(sheetName, wholeContent))
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE)
                .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                .setHeadingPath(headingPath)
                .setPageStart(element.getPage())
                .setPageEnd(element.getPage())
                .setEmbedEnabled(true));

        MarkdownTableChunkSupport.TableChunkArtifacts artifacts =
                MarkdownTableChunkSupport.parse(tableMarkdown, sheetName);
        if (config.isTableRowIndexEnabled() && artifacts.hasRows()) {
            int rowIndex = 1;
            for (String rowJson : artifacts.getRowJsonLines()) {
                String rowContent = sheetName + " Row" + rowIndex + " " + rowJson;
                chunks.add(new AiKnowledgeChunkBO()
                        .setContent(rowContent.trim())
                        .setEmbedText(buildEmbedText(sheetName, rowContent))
                        .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW)
                        .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                        .setHeadingPath(headingPath + ":Row" + rowIndex)
                        .setPageStart(element.getPage())
                        .setPageEnd(element.getPage())
                        .setEmbedEnabled(true));
                rowIndex++;
            }
        }
        if (config.isTableSummaryEnabled() && StrUtil.isNotBlank(artifacts.getSummary())) {
            chunks.add(new AiKnowledgeChunkBO()
                    .setContent(artifacts.getSummary())
                    .setEmbedText(buildEmbedText(sheetName, artifacts.getSummary()))
                    .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_SUMMARY)
                    .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                    .setHeadingPath(headingPath)
                    .setPageStart(element.getPage())
                    .setPageEnd(element.getPage())
                    .setEmbedEnabled(true));
        }
        return chunks;
    }

    private static String buildEmbedText(String sheetName, String content) {
        return "Sheet: " + sheetName + "\n" + content;
    }

}
