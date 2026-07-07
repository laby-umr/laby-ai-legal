package com.laby.module.ai.framework.ai.rerank;

import lombok.Data;

@Data
public class DashScopeRerankProperties {

    private String apiKey;

    /** 默认 gte-rerank-v2，与 Spring AI Alibaba 默认一致 */
    private String rerankModel = "gte-rerank-v2";

}
