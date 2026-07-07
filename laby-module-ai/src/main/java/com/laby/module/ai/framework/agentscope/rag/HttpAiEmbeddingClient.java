package com.laby.module.ai.framework.agentscope.rag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laby.module.ai.core.rag.AiEmbeddingClient;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.ai.enums.ErrorCodeConstants.MODEL_NOT_EXISTS;

/**
 * P0 平台 Embedding HTTP 客户端（OpenAI-compatible / DashScope compatible-mode）
 */
public class HttpAiEmbeddingClient implements AiEmbeddingClient {

    private final AiPlatformEnum platform;
    private final String modelName;
    private final String apiKey;
    private final String embeddingsUrl;

    public HttpAiEmbeddingClient(AgentScopeModelConfig config) {
        this.platform = config.getPlatform();
        this.modelName = config.getModelName();
        this.apiKey = config.getApiKey();
        this.embeddingsUrl = resolveEmbeddingsUrl(platform, config.getBaseUrl());
        validatePlatform();
    }

    @Override
    public float[] embed(String text) {
        List<float[]> vectors = embedBatch(List.of(text));
        return vectors.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (CollUtil.isEmpty(texts)) {
            return List.of();
        }
        JSONObject body = new JSONObject();
        body.set("model", modelName);
        if (texts.size() == 1) {
            body.set("input", texts.get(0));
        } else {
            body.set("input", texts);
        }

        HttpResponse response = HttpRequest.post(embeddingsUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(60_000)
                .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(StrUtil.format(
                    "[HttpAiEmbeddingClient] embedding 失败 platform={} status={} body={}",
                    platform.getPlatform(), response.getStatus(), response.body()));
        }

        JSONObject json = JSONUtil.parseObj(response.body());
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("[HttpAiEmbeddingClient] embedding 响应无 data: " + response.body());
        }

        List<IndexedVector> indexed = new ArrayList<>(data.size());
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            JSONArray embedding = item.getJSONArray("embedding");
            indexed.add(new IndexedVector(item.getInt("index", i), toFloatArray(embedding)));
        }
        indexed.sort(Comparator.comparingInt(IndexedVector::index));
        return indexed.stream().map(IndexedVector::vector).toList();
    }

    private static float[] toFloatArray(JSONArray embedding) {
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.getFloat(i);
        }
        return vector;
    }

    private void validatePlatform() {
        switch (platform) {
            case TONG_YI, OPENAI, DEEP_SEEK, ZHI_PU, OLLAMA -> {
            }
            default -> throw exception(MODEL_NOT_EXISTS);
        }
    }

    private static String resolveEmbeddingsUrl(AiPlatformEnum platform, String baseUrl) {
        return switch (platform) {
            case TONG_YI -> "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
            case OPENAI -> joinUrl(StrUtil.blankToDefault(baseUrl, "https://api.openai.com/v1"), "embeddings");
            case DEEP_SEEK -> joinUrl(StrUtil.blankToDefault(baseUrl, "https://api.deepseek.com/v1"), "embeddings");
            case ZHI_PU -> joinUrl(StrUtil.blankToDefault(baseUrl, "https://open.bigmodel.cn/api/paas/v4"), "embeddings");
            case OLLAMA -> joinUrl(normalizeOllamaBaseUrl(baseUrl), "embeddings");
            default -> throw new IllegalArgumentException("Unsupported embedding platform: " + platform);
        };
    }

    private static String normalizeOllamaBaseUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "http://127.0.0.1:11434/v1";
        }
        return url.endsWith("/v1") ? url : url.replaceAll("/+$", "") + "/v1";
    }

    private static String joinUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private record IndexedVector(int index, float[] vector) {
    }

}
