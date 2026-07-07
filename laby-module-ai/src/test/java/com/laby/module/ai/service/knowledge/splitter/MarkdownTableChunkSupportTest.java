package com.laby.module.ai.service.knowledge.splitter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTableChunkSupportTest {

    @Test
    void parse_extractsRowJsonAndSummary() {
        String markdown = """
                |期次|比例|条件|
                |---|---|---|
                |签约|30%|合同生效后7日|
                |验收|70%|验收合格后|
                """;
        MarkdownTableChunkSupport.TableChunkArtifacts artifacts =
                MarkdownTableChunkSupport.parse(markdown, "付款表");

        assertEquals(2, artifacts.getRowJsonLines().size());
        assertTrue(artifacts.getRowJsonLines().get(0).contains("\"期次\":\"签约\""));
        assertTrue(artifacts.getSummary().contains("付款表"));
        assertTrue(artifacts.getSummary().contains("2 行"));
    }

}
