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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 结构化层级分片器（Parent-Child + 元素类型路由）
 */
public class HierarchicalKnowledgeChunker {

    private final DocumentParseProperties.StructuredChunkConfig config;
    private final SemanticTextSplitter textSplitter;

    public HierarchicalKnowledgeChunker(DocumentParseProperties properties) {
        this.config = properties.getStructuredChunk();
        this.textSplitter = new SemanticTextSplitter(
                config.getChildMaxTokens(), config.getChildOverlapTokens());
    }

    public List<AiKnowledgeChunkBO> chunk(AiStructuredDocumentParseResult parseResult) {
        if (parseResult == null || !parseResult.supportsStructuredChunking()) {
            return chunkPlainText(parseResult != null ? parseResult.getMarkdown() : "");
        }
        List<AiKnowledgeChunkBO> chunks = new ArrayList<>();
        AtomicInteger sectionCounter = new AtomicInteger();
        StringBuilder parentBuffer = new StringBuilder();
        String currentParentKey = null;
        List<String> headingStack = new ArrayList<>();

        AiStructuredDocument document = parseResult.getStructuredDocument();
        for (AiStructuredDocumentElement element : document.getElements()) {
            AiStructuredDocumentElementTypeEnum type =
                    AiStructuredDocumentElementTypeEnum.valueOfCode(element.getType());
            switch (type) {
                case TITLE -> {
                    flushParent(chunks, parentBuffer, currentParentKey, headingStack);
                    updateHeadingStack(headingStack, element);
                    currentParentKey = "section-" + sectionCounter.incrementAndGet();
                    parentBuffer.setLength(0);
                    appendParentLine(parentBuffer, element.getText());
                }
                case TABLE -> chunks.addAll(buildTableChunks(element, headingStack));
                case IMAGE -> chunks.add(buildImageChunk(element, headingStack));
                case FORMULA -> chunks.add(buildFormulaChunk(element, headingStack));
                case TEXT -> {
                    appendParentLine(parentBuffer, element.getText());
                    chunks.addAll(buildTextChildChunks(element, headingStack, currentParentKey));
                }
            }
        }
        flushParent(chunks, parentBuffer, currentParentKey, headingStack);
        return chunks.stream().filter(chunk -> StrUtil.isNotBlank(chunk.getContent())).toList();
    }

    private List<AiKnowledgeChunkBO> chunkPlainText(String markdown) {
        if (StrUtil.isBlank(markdown)) {
            return List.of();
        }
        List<AiKnowledgeChunkBO> chunks = new ArrayList<>();
        for (String segment : textSplitter.split(markdown)) {
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

    private void flushParent(List<AiKnowledgeChunkBO> chunks, StringBuilder parentBuffer,
                             String parentTempKey, List<String> headingStack) {
        if (parentTempKey == null || parentBuffer.isEmpty()) {
            return;
        }
        String content = parentBuffer.toString().trim();
        if (StrUtil.isBlank(content)) {
            return;
        }
        chunks.add(new AiKnowledgeChunkBO()
                .setContent(content)
                .setEmbedText(buildEmbedText(headingStack, content))
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TEXT)
                .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.PARENT)
                .setTempKey(parentTempKey)
                .setHeadingPath(joinHeadingPath(headingStack))
                .setEmbedEnabled(config.isEmbedParent()));
    }

    private List<AiKnowledgeChunkBO> buildTextChildChunks(AiStructuredDocumentElement element,
                                                          List<String> headingStack,
                                                          String parentTempKey) {
        if (StrUtil.isBlank(element.getText())) {
            return List.of();
        }
        List<AiKnowledgeChunkBO> chunks = new ArrayList<>();
        for (String segment : textSplitter.split(element.getText())) {
            if (StrUtil.isBlank(segment)) {
                continue;
            }
            chunks.add(new AiKnowledgeChunkBO()
                    .setContent(segment)
                    .setEmbedText(buildEmbedText(headingStack, segment))
                    .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TEXT)
                    .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                    .setParentTempKey(parentTempKey)
                    .setHeadingPath(joinHeadingPath(headingStack))
                    .setPageStart(element.getPage())
                    .setPageEnd(element.getPage())
                    .setEmbedEnabled(true));
        }
        return chunks;
    }

    private List<AiKnowledgeChunkBO> buildTableChunks(AiStructuredDocumentElement element, List<String> headingStack) {
        String tableMarkdown = StrUtil.blankToDefault(element.getMarkdown(), element.getText());
        String caption = StrUtil.blankToDefault(element.getCaption(), "表格");
        String wholeContent = StrUtil.isNotBlank(caption)
                ? caption + "\n" + tableMarkdown
                : tableMarkdown;
        String headingPath = joinHeadingPath(headingStack);

        List<AiKnowledgeChunkBO> chunks = new ArrayList<>();
        chunks.add(new AiKnowledgeChunkBO()
                .setContent(wholeContent.trim())
                .setEmbedText(buildEmbedText(headingStack, wholeContent))
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE)
                .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                .setHeadingPath(headingPath)
                .setPageStart(element.getPage())
                .setPageEnd(element.getPage())
                .setEmbedEnabled(true));

        MarkdownTableChunkSupport.TableChunkArtifacts artifacts =
                MarkdownTableChunkSupport.parse(tableMarkdown, caption);
        if (config.isTableRowIndexEnabled() && artifacts.hasRows()) {
            for (String rowJson : artifacts.getRowJsonLines()) {
                String rowContent = caption + " " + rowJson;
                chunks.add(new AiKnowledgeChunkBO()
                        .setContent(rowContent.trim())
                        .setEmbedText(buildEmbedText(headingStack, rowContent))
                        .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_ROW)
                        .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                        .setHeadingPath(headingPath)
                        .setPageStart(element.getPage())
                        .setPageEnd(element.getPage())
                        .setEmbedEnabled(true));
            }
        }
        if (config.isTableSummaryEnabled() && StrUtil.isNotBlank(artifacts.getSummary())) {
            chunks.add(new AiKnowledgeChunkBO()
                    .setContent(artifacts.getSummary())
                    .setEmbedText(buildEmbedText(headingStack, artifacts.getSummary()))
                    .setBlockType(AiKnowledgeSegmentBlockTypeEnum.TABLE_SUMMARY)
                    .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                    .setHeadingPath(headingPath)
                    .setPageStart(element.getPage())
                    .setPageEnd(element.getPage())
                    .setEmbedEnabled(true));
        }
        return chunks;
    }

    private AiKnowledgeChunkBO buildImageChunk(AiStructuredDocumentElement element, List<String> headingStack) {
        StringBuilder content = new StringBuilder();
        if (StrUtil.isNotBlank(element.getCaption())) {
            content.append(element.getCaption());
        }
        if (StrUtil.isNotBlank(element.getDescription())) {
            if (!content.isEmpty()) {
                content.append("\n");
            }
            content.append(element.getDescription());
        }
        if (StrUtil.isNotBlank(element.getText())) {
            if (!content.isEmpty()) {
                content.append("\n");
            }
            content.append(element.getText());
        }
        String body = content.toString().trim();
        return new AiKnowledgeChunkBO()
                .setContent(body)
                .setEmbedText(buildEmbedText(headingStack, body))
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.IMAGE)
                .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                .setHeadingPath(joinHeadingPath(headingStack))
                .setPageStart(element.getPage())
                .setPageEnd(element.getPage())
                .setEmbedEnabled(true);
    }

    private AiKnowledgeChunkBO buildFormulaChunk(AiStructuredDocumentElement element, List<String> headingStack) {
        String body = "[公式] " + StrUtil.blankToDefault(element.getText(), "");
        return new AiKnowledgeChunkBO()
                .setContent(body.trim())
                .setEmbedText(buildEmbedText(headingStack, body))
                .setBlockType(AiKnowledgeSegmentBlockTypeEnum.FORMULA)
                .setChunkLevel(AiKnowledgeSegmentChunkLevelEnum.CHILD)
                .setHeadingPath(joinHeadingPath(headingStack))
                .setPageStart(element.getPage())
                .setPageEnd(element.getPage())
                .setEmbedEnabled(true);
    }

    private static void updateHeadingStack(List<String> headingStack, AiStructuredDocumentElement element) {
        int level = element.getLevel() != null && element.getLevel() > 0 ? element.getLevel() : 2;
        while (headingStack.size() >= level) {
            headingStack.remove(headingStack.size() - 1);
        }
        headingStack.add(StrUtil.blankToDefault(element.getText(), ""));
    }

    private static void appendParentLine(StringBuilder parentBuffer, String text) {
        if (StrUtil.isBlank(text)) {
            return;
        }
        if (!parentBuffer.isEmpty()) {
            parentBuffer.append("\n");
        }
        parentBuffer.append(text.trim());
    }

    private static String joinHeadingPath(List<String> headingStack) {
        return headingStack.stream()
                .filter(StrUtil::isNotBlank)
                .reduce((a, b) -> a + " > " + b)
                .orElse("");
    }

    private static String buildEmbedText(List<String> headingStack, String content) {
        String headingPath = joinHeadingPath(headingStack);
        if (StrUtil.isBlank(headingPath)) {
            return content;
        }
        return headingPath + "\n" + content;
    }

}
