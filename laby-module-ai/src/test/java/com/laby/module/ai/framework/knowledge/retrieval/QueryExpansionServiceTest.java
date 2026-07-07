package com.laby.module.ai.framework.knowledge.retrieval;

import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExpansionServiceTest {

    private QueryExpansionService service;

    @BeforeEach
    void setUp() {
        KnowledgeRetrievalProperties properties = new KnowledgeRetrievalProperties();
        properties.getMultiQuery().setEnabled(true);
        properties.getMultiQuery().setMaxVariants(3);
        service = new QueryExpansionService(properties);
        service.loadDictionary();
    }

    @Test
    void expand_translatesProfitAndAge() {
        List<String> variants = service.expand("六月 Profit", AiQueryIntentEnum.TABLE_CELL);

        assertTrue(variants.get(0).contains("Profit") || variants.get(0).contains("利润"));
        assertTrue(variants.stream().anyMatch(item -> item.toLowerCase().contains("profit")
                || item.contains("利润")));
        assertEquals(3, variants.size());
    }

    @Test
    void expand_addsTableSuffixForTableIntent() {
        List<String> variants = service.expand("John Doe 年龄", AiQueryIntentEnum.TABLE_CELL);

        assertTrue(variants.stream().anyMatch(item -> item.contains("表格") || item.toLowerCase().contains("table")));
    }

    @Test
    void expand_respectsMaxVariants() {
        List<String> variants = service.expand("付款条款", AiQueryIntentEnum.GENERAL);

        assertEquals(3, variants.size());
    }

}
