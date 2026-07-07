package com.laby.module.ai.framework.knowledge.retrieval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparseTextNormalizerTest {

    @Test
    void normalize_stripsMarkdownAndTableSymbols() {
        String input = "| John | Doe | 99 |";
        String normalized = SparseTextNormalizer.normalize(input);

        assertEquals("John Doe 99", normalized);
    }

    @Test
    void normalize_removesHeadingMarkers() {
        String normalized = SparseTextNormalizer.normalize("## 利润 Summary");

        assertTrue(normalized.contains("利润"));
        assertTrue(normalized.contains("Summary"));
        assertTrue(!normalized.contains("#"));
    }

    @Test
    void normalize_collapsesWhitespace() {
        assertEquals("John Doe Age", SparseTextNormalizer.normalize("  John   Doe   Age  "));
    }

}
