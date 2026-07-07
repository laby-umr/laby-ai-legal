package com.laby.module.ai.framework.knowledge.retrieval;

import com.laby.module.ai.enums.knowledge.AiQueryIntentEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryIntentClassifierTest {

    @Test
    void classify_tableCellWhenAgeOrProfitMentioned() {
        assertEquals(AiQueryIntentEnum.TABLE_CELL, QueryIntentClassifier.classify("John Doe 的年龄"));
        assertEquals(AiQueryIntentEnum.TABLE_CELL, QueryIntentClassifier.classify("June Profit"));
        assertEquals(AiQueryIntentEnum.TABLE_CELL, QueryIntentClassifier.classify("哪一行是张三"));
    }

    @Test
    void classify_tableOverviewWhenSummaryKeywords() {
        assertEquals(AiQueryIntentEnum.TABLE_OVERVIEW, QueryIntentClassifier.classify("总结这个表格"));
        assertEquals(AiQueryIntentEnum.TABLE_OVERVIEW, QueryIntentClassifier.classify("有哪些列"));
    }

    @Test
    void classify_sectionWhenChapterOrSlideMentioned() {
        assertEquals(AiQueryIntentEnum.SECTION, QueryIntentClassifier.classify("第三章讲什么"));
        assertEquals(AiQueryIntentEnum.SECTION, QueryIntentClassifier.classify("slide 5 内容"));
    }

    @Test
    void classify_entityWhenEnglishNamePresent() {
        assertEquals(AiQueryIntentEnum.ENTITY, QueryIntentClassifier.classify("John Doe"));
    }

    @Test
    void classify_generalByDefault() {
        assertEquals(AiQueryIntentEnum.GENERAL, QueryIntentClassifier.classify("介绍一下系统"));
    }

}
