package com.laby.module.legal.service.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalContractMemoryFactExtractorTest {

    @Test
    void parseMemoryLines_shouldParseTypedBulletList() {
        List<LegalContractMemoryFactExtractor.ExtractedMemory> memories =
                LegalContractMemoryFactExtractor.parseMemoryLines("""
                        - [fact] 付款周期为 30 天
                        - [risk] 违约金上限 10%
                        """);

        assertEquals(2, memories.size());
        assertEquals("fact", memories.get(0).memoryType());
        assertEquals("付款周期为 30 天", memories.get(0).content());
        assertEquals("risk", memories.get(1).memoryType());
    }

    @Test
    void parseMemoryLines_noneReturnsEmpty() {
        assertTrue(LegalContractMemoryFactExtractor.parseMemoryLines("NONE").isEmpty());
    }

    @Test
    void parseMemoryLine_shouldDefaultToFactWithoutTag() {
        LegalContractMemoryFactExtractor.ExtractedMemory memory =
                LegalContractMemoryFactExtractor.parseMemoryLine("纯文本事实");
        assertEquals("fact", memory.memoryType());
        assertEquals("纯文本事实", memory.content());
    }

}
