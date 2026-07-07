package com.laby.module.ai.framework.agentscope.rag;

import lombok.Data;

@Data
public class QdrantVectorStoreProperties {

    private String host = "127.0.0.1";
    private int port = 6334;
    private boolean useTls = false;
    private String apiKey;
    private String collectionName = "knowledge_segment";
    private boolean initializeSchema = false;
    /** 与 Spring AI QdrantVectorStore 默认一致，保证存量数据兼容 */
    private String contentFieldName = "doc_content";

}
