package com.laby.module.ai.framework.ai.rerank;



import cn.hutool.core.util.StrUtil;

import com.laby.framework.common.util.collection.CollectionUtils;

import com.laby.module.ai.core.rag.AiVectorSearchHit;



import java.util.List;



/**

 * 向量检索命中 Rerank 适配器（DashScope HTTP）

 */

public final class AiVectorHitReranker {



    private AiVectorHitReranker() {

    }



    public static List<AiVectorSearchHit> rerank(DashScopeRerankClient rerankClient, String query,

                                                 List<AiVectorSearchHit> hits, int topK,

                                                 double similarityThreshold) {

        List<String> documents = CollectionUtils.convertList(hits,

                hit -> StrUtil.nullToDefault(hit.getContent(), ""));

        List<DashScopeRerankClient.RerankResult> results = rerankClient.rerank(query, documents, topK);

        return CollectionUtils.convertList(results, result -> {

            if (result.getScore() < similarityThreshold) {

                return null;

            }

            AiVectorSearchHit original = hits.get(result.getIndex());

            return new AiVectorSearchHit()

                    .setId(original.getId())

                    .setScore(result.getScore())

                    .setContent(original.getContent())

                    .setMetadata(original.getMetadata());

        });

    }



}

