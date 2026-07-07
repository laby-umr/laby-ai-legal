package com.laby.module.ai.enums.knowledge;



/**

 * 知识库模块常量

 */

public final class AiKnowledgeConstants {



    /** 无引用守卫：严格拒答文案 */

    public static final String NO_ANSWER_STRICT_REPLY =

            "知识库中未找到相关内容，请尝试补充文档名称或更具体的关键词。";



    /** 无引用守卫：提示补充关键词文案 */

    public static final String NO_ANSWER_HINT_REPLY =

            "知识库中未找到足够相关的内容，请补充文档名称、章节或更具体的关键词后重试。";



    private AiKnowledgeConstants() {

    }



}

