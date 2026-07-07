package com.laby.module.ai.core.rag;

import java.util.List;

public interface AiVectorStoreClient {

    /**
     * 写入向量文档。若 doc.id 为空则生成 UUID 并回填。
     *
     * @return 各文档 id，逗号分隔
     */
    String add(List<AiVectorDocument> docs);

    void delete(List<String> ids);

    List<AiVectorSearchHit> search(AiVectorSearchRequest req);

    /**
     * 批量检查 point 是否存在并读取 payload metadata（不含向量体）。
     *
     * @param ids Qdrant point UUID
     * @return 与入参顺序对应的检查结果；非法 UUID 视为不存在
     */
    java.util.List<AiVectorPointInfo> retrievePoints(java.util.List<String> ids);

}
