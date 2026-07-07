package com.laby.module.ai.framework.ai.rerank;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DashScope 文本 Rerank HTTP 客户端
 */
public class DashScopeRerankClient {

    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    private final DashScopeRerankProperties properties;

    public DashScopeRerankClient(DashScopeRerankProperties properties) {
        this.properties = properties;
    }

    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        if (StrUtil.isBlank(query) || documents == null || documents.isEmpty()) {
            return List.of();
        }

        JSONObject input = new JSONObject();
        input.set("query", query);
        input.set("documents", documents);

        JSONObject parameters = new JSONObject();
        parameters.set("top_n", topN);
        parameters.set("return_documents", false);

        JSONObject body = new JSONObject();
        body.set("model", properties.getRerankModel());
        body.set("input", input);
        body.set("parameters", parameters);

        HttpResponse response = HttpRequest.post(RERANK_URL)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(60_000)
                .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(StrUtil.format(
                    "[DashScopeRerankClient] rerank 失败 status={} body={}",
                    response.getStatus(), response.body()));
        }

        JSONObject json = JSONUtil.parseObj(response.body());
        JSONObject output = json.getJSONObject("output");
        if (output == null) {
            throw new IllegalStateException("[DashScopeRerankClient] rerank 响应无 output: " + response.body());
        }
        JSONArray results = output.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<RerankResult> rerankResults = new ArrayList<>(results.size());
        for (int i = 0; i < results.size(); i++) {
            JSONObject item = results.getJSONObject(i);
            rerankResults.add(new RerankResult(
                    item.getInt("index"),
                    item.getDouble("relevance_score")));
        }
        return rerankResults;
    }

    @Data
    @AllArgsConstructor
    public static class RerankResult {
        private int index;
        private double score;
    }

}
