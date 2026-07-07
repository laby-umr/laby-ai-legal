package com.laby.module.ai.service.knowledge.support;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.enums.AiDocumentSplitStrategyEnum;
import com.laby.module.ai.service.knowledge.splitter.KnowledgeTextSplitter;
import com.laby.module.ai.service.knowledge.splitter.MarkdownQaSplitter;
import com.laby.module.ai.service.knowledge.splitter.SemanticTextSplitter;
import com.laby.module.ai.service.knowledge.splitter.TokenKnowledgeTextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库文档切片策略与文本拆分。
 */
@Slf4j
@Component
public class AiKnowledgeSegmentSplitSupport {

    @SuppressWarnings("EnhancedSwitchMigration")
    public List<String> splitByStrategy(String content, Integer segmentMaxTokens,
                                        AiDocumentSplitStrategyEnum strategy, String url) {
        if (strategy == AiDocumentSplitStrategyEnum.AUTO) {
            strategy = detectStrategy(content, url);
            log.info("[splitByStrategy][自动检测到文档策略: {}]", strategy.getName());
        }
        KnowledgeTextSplitter textSplitter;
        switch (strategy) {
            case MARKDOWN_QA:
                textSplitter = new MarkdownQaSplitter(segmentMaxTokens);
                break;
            case SEMANTIC:
                textSplitter = new SemanticTextSplitter(segmentMaxTokens);
                break;
            case PARAGRAPH:
                textSplitter = new SemanticTextSplitter(segmentMaxTokens, 0);
                break;
            case TOKEN:
            default:
                textSplitter = new TokenKnowledgeTextSplitter(segmentMaxTokens);
                break;
        }
        return textSplitter.split(content);
    }

    public AiDocumentSplitStrategyEnum detectStrategy(String content, String url) {
        if (StrUtil.isEmpty(content)) {
            return AiDocumentSplitStrategyEnum.TOKEN;
        }
        if (isMarkdownQaFormat(content, url)) {
            return AiDocumentSplitStrategyEnum.MARKDOWN_QA;
        }
        if (isMarkdownDocument(url)) {
            return AiDocumentSplitStrategyEnum.SEMANTIC;
        }
        return AiDocumentSplitStrategyEnum.SEMANTIC;
    }

    private static boolean isMarkdownQaFormat(String content, String url) {
        if (StrUtil.isNotEmpty(url) && !url.toLowerCase().endsWith(".md")) {
            return false;
        }
        long h2Count = content.lines()
                .filter(line -> line.trim().startsWith("## "))
                .count();
        if (h2Count < 2) {
            return false;
        }
        long totalLines = content.lines().count();
        double h2Ratio = (double) h2Count / totalLines;
        return h2Ratio > 0.1;
    }

    private static boolean isMarkdownDocument(String url) {
        return StrUtil.endWithAnyIgnoreCase(url, ".md", ".markdown");
    }

}
