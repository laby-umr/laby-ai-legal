package com.laby.module.ai.service.knowledge.splitter;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.token.AiTokenCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Token 上限的文本切片器（替代 Spring AI TokenTextSplitter）
 */
public class TokenKnowledgeTextSplitter implements KnowledgeTextSplitter {

    private static final String PARAGRAPH_SEPARATOR = "\n\n";

    private final int chunkSize;

    public TokenKnowledgeTextSplitter(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    public List<String> split(String text) {
        if (StrUtil.isEmpty(text)) {
            return Collections.emptyList();
        }
        if (AiTokenCounter.estimate(text) <= chunkSize) {
            return Collections.singletonList(text.trim());
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split(PARAGRAPH_SEPARATOR);
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        for (String paragraph : paragraphs) {
            if (StrUtil.isEmpty(paragraph)) {
                continue;
            }
            int paragraphTokens = AiTokenCounter.estimate(paragraph);
            if (paragraphTokens > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
                chunks.addAll(splitLongParagraph(paragraph));
                continue;
            }
            if (currentTokens + paragraphTokens > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                currentTokens = 0;
            }
            if (currentChunk.length() > 0) {
                currentChunk.append(PARAGRAPH_SEPARATOR);
            }
            currentChunk.append(paragraph);
            currentTokens += paragraphTokens;
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks.isEmpty() ? Collections.singletonList(text.trim()) : chunks;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = findChunkEnd(paragraph, start);
            String chunk = paragraph.substring(start, end).trim();
            if (StrUtil.isNotEmpty(chunk)) {
                chunks.add(chunk);
            }
            if (end >= paragraph.length()) {
                break;
            }
            start = end;
        }
        return chunks.isEmpty() ? Collections.singletonList(paragraph.trim()) : chunks;
    }

    private int findChunkEnd(String text, int start) {
        if (AiTokenCounter.estimate(text.substring(start)) <= chunkSize) {
            return text.length();
        }
        int low = start + 1;
        int high = text.length();
        int best = low;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (AiTokenCounter.estimate(text.substring(start, mid)) <= chunkSize) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

}
