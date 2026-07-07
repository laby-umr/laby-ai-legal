package com.laby.module.ai.service.knowledge.splitter;

import java.util.List;

/**
 * 知识库文本切片器
 */
public interface KnowledgeTextSplitter {

    List<String> split(String text);

}
