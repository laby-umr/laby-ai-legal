package com.laby.module.ai.framework.agentscope.image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laby.module.ai.core.image.AiImageClient;
import com.laby.module.ai.core.image.AiImageGenerateRequest;
import com.laby.module.ai.core.image.AiImageGenerateResult;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.module.ai.framework.agentscope.auth.BaiduQianfanAuth;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelConfig;
import com.laby.module.ai.framework.ai.core.model.siliconflow.SiliconFlowApiConstants;

import java.util.Map;

/**
 * Image HTTP 客户端（OpenAI / DashScope / 智谱 / 硅基流动 / Stability / 文心）
 */
public class HttpAiImageClient implements AiImageClient {

    private static final String STABILITY_DEFAULT_BASE_URL = "https://api.stability.ai";
    private static final String QIANFAN_IMAGE_URL = "https://qianfan.baidubce.com/v2/images/generations";
    private static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String DASHSCOPE_IMAGE_SYNTHESIS_PATH =
            "/services/aigc/text2image/image-synthesis";
    private static final String DASHSCOPE_IMAGE_GENERATION_PATH =
            "/services/aigc/image-generation/generation";
    private static final int IMAGE_HTTP_TIMEOUT_MS = 120_000;
    private static final int TASK_POLL_INTERVAL_MS = 2_000;
    private static final int TASK_POLL_MAX_ATTEMPTS = 60;

    private final AiPlatformEnum platform;
    private final String apiKey;
    private final String baseUrl;

    public HttpAiImageClient(AgentScopeModelConfig config) {
        this.platform = config.getPlatform();
        this.apiKey = config.getApiKey();
        this.baseUrl = config.getBaseUrl();
        validatePlatform();
    }

    @Override
    public AiImageGenerateResult generate(AiImageGenerateRequest request) {
        return switch (platform) {
            case OPENAI -> generateOpenAi(request);
            case TONG_YI -> generateTongYi(request);
            case ZHI_PU -> generateZhiPu(request);
            case SILICON_FLOW -> generateSiliconFlow(request);
            case STABLE_DIFFUSION -> generateStability(request);
            case YI_YAN -> generateYiYan(request);
            default -> throw unsupported(platform.getName());
        };
    }

    private static UnsupportedOperationException unsupported(String platformName) {
        return new UnsupportedOperationException(
                StrUtil.format("图像生成平台 [{}] 尚未支持", platformName));
    }

    private AiImageGenerateResult generateOpenAi(AiImageGenerateRequest request) {
        JSONObject body = new JSONObject();
        body.set("model", request.getModel());
        body.set("prompt", request.getPrompt());
        body.set("n", 1);
        body.set("size", request.getWidth() + "x" + request.getHeight());
        body.set("response_format", "b64_json");
        String style = getOption(request.getOptions(), "style");
        if (StrUtil.isNotBlank(style)) {
            body.set("style", style);
        }

        String url = joinUrl(StrUtil.blankToDefault(baseUrl, "https://api.openai.com/v1"), "images/generations");
        JSONObject json = postJson(url, body, Map.of());
        return parseOpenAiCompatibleResponse(json);
    }

    private AiImageGenerateResult generateTongYi(AiImageGenerateRequest request) {
        boolean messagesFormat = usesDashScopeMessagesFormat(request.getModel());
        String submitUrl = joinUrl(DASHSCOPE_BASE_URL,
                messagesFormat ? DASHSCOPE_IMAGE_GENERATION_PATH : DASHSCOPE_IMAGE_SYNTHESIS_PATH);

        JSONObject body = new JSONObject();
        body.set("model", request.getModel());
        if (messagesFormat) {
            JSONObject textContent = new JSONObject().set("text", request.getPrompt());
            JSONObject message = new JSONObject()
                    .set("role", "user")
                    .set("content", JSONUtil.createArray().put(textContent));
            body.set("input", new JSONObject().set("messages", JSONUtil.createArray().put(message)));
        } else {
            body.set("input", new JSONObject().set("prompt", request.getPrompt()));
        }
        JSONObject parameters = new JSONObject()
                .set("size", request.getWidth() + "*" + request.getHeight())
                .set("n", 1);
        body.set("parameters", parameters);

        JSONObject submitResponse = postJson(submitUrl, body, Map.of("X-DashScope-Async", "enable"));
        String taskId = extractDashScopeTaskId(submitResponse);
        if (StrUtil.isBlank(taskId)) {
            return parseDashScopeInlineResponse(submitResponse);
        }

        String taskUrl = joinUrl(DASHSCOPE_BASE_URL, "tasks/" + taskId);
        for (int attempt = 0; attempt < TASK_POLL_MAX_ATTEMPTS; attempt++) {
            JSONObject taskResponse = getJson(taskUrl);
            JSONObject output = taskResponse.getJSONObject("output");
            if (output == null) {
                throw new IllegalStateException("[HttpAiImageClient] DashScope 任务响应无 output: " + taskResponse);
            }
            String taskStatus = output.getStr("task_status");
            if ("SUCCEEDED".equalsIgnoreCase(taskStatus)) {
                return parseDashScopeOutput(output);
            }
            if ("FAILED".equalsIgnoreCase(taskStatus)) {
                throw new IllegalStateException(StrUtil.format(
                        "[HttpAiImageClient] DashScope 图像任务失败: {}", output.getStr("message", taskResponse.toString())));
            }
            sleepQuietly(TASK_POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("[HttpAiImageClient] DashScope 图像任务轮询超时, taskId=" + taskId);
    }

    private AiImageGenerateResult generateSiliconFlow(AiImageGenerateRequest request) {
        JSONObject body = new JSONObject();
        body.set("model", request.getModel());
        body.set("prompt", request.getPrompt());
        body.set("batch_size", 1);
        putIfNotBlank(body, "negative_prompt", getOption(request.getOptions(), "negative_prompt", "negativePrompt"));
        putIfInteger(body, "seed", getOption(request.getOptions(), "seed"));
        putIfFloat(body, "guidance_scale", getOption(request.getOptions(), "guidance_scale", "cfgScale"));
        putIfInteger(body, "num_inference_steps", getOption(request.getOptions(), "num_inference_steps", "steps"));

        String url = joinUrl(StrUtil.blankToDefault(baseUrl, SiliconFlowApiConstants.DEFAULT_BASE_URL), "v1/images/generations");
        JSONObject json = postJson(url, body, Map.of());
        return parseOpenAiCompatibleResponse(json);
    }

    private AiImageGenerateResult generateStability(AiImageGenerateRequest request) {
        String engineId = StrUtil.blankToDefault(request.getModel(), "stable-diffusion-v1-6");
        JSONObject textPrompt = new JSONObject().set("text", request.getPrompt()).set("weight", 1);
        JSONObject body = new JSONObject()
                .set("text_prompts", JSONUtil.createArray().put(textPrompt))
                .set("cfg_scale", parseFloatOption(request.getOptions(), 7F, "cfgScale", "cfg_scale"))
                .set("height", request.getHeight())
                .set("width", request.getWidth())
                .set("samples", 1)
                .set("steps", parseIntOption(request.getOptions(), 30, "steps", "num_inference_steps"));
        putIfInteger(body, "seed", getOption(request.getOptions(), "seed"));

        String url = joinUrl(StrUtil.blankToDefault(baseUrl, STABILITY_DEFAULT_BASE_URL),
                "v1/generation/" + engineId + "/text-to-image");
        JSONObject json = postJson(url, body, Map.of());
        JSONArray artifacts = json.getJSONArray("artifacts");
        if (artifacts == null || artifacts.isEmpty()) {
            throw new IllegalStateException("[HttpAiImageClient] Stability 响应无 artifacts: " + json);
        }
        String b64 = artifacts.getJSONObject(0).getStr("base64");
        if (StrUtil.isBlank(b64)) {
            throw new IllegalStateException("[HttpAiImageClient] Stability 响应无 base64: " + json);
        }
        return new AiImageGenerateResult().setB64Json(b64);
    }

    private AiImageGenerateResult generateYiYan(AiImageGenerateRequest request) {
        JSONObject body = new JSONObject();
        body.set("model", request.getModel());
        body.set("prompt", request.getPrompt());
        body.set("n", 1);
        if (request.getWidth() != null && request.getHeight() != null) {
            body.set("size", request.getWidth() + "x" + request.getHeight());
        }

        String bearer = BaiduQianfanAuth.resolveBearerToken(apiKey);
        JSONObject json = postJson(QIANFAN_IMAGE_URL, body, Map.of(), bearer);
        return parseOpenAiCompatibleResponse(json);
    }

    private AiImageGenerateResult generateZhiPu(AiImageGenerateRequest request) {
        JSONObject body = new JSONObject();
        body.set("model", request.getModel());
        body.set("prompt", request.getPrompt());
        if (request.getWidth() != null && request.getHeight() != null) {
            body.set("size", request.getWidth() + "x" + request.getHeight());
        }

        String url = joinUrl(StrUtil.blankToDefault(baseUrl, "https://open.bigmodel.cn/api/paas/v4"),
                "images/generations");
        JSONObject json = postJson(url, body, Map.of());
        return parseOpenAiCompatibleResponse(json);
    }

    private static boolean usesDashScopeMessagesFormat(String model) {
        if (StrUtil.isBlank(model)) {
            return false;
        }
        String lowerModel = model.toLowerCase();
        return lowerModel.contains("qwen-image")
                || lowerModel.startsWith("wan2.6")
                || lowerModel.startsWith("wan2.7");
    }

    private static String extractDashScopeTaskId(JSONObject response) {
        JSONObject output = response.getJSONObject("output");
        if (output != null && StrUtil.isNotBlank(output.getStr("task_id"))) {
            return output.getStr("task_id");
        }
        return response.getStr("task_id");
    }

    private static AiImageGenerateResult parseDashScopeInlineResponse(JSONObject response) {
        JSONObject output = response.getJSONObject("output");
        if (output == null) {
            throw new IllegalStateException("[HttpAiImageClient] DashScope 响应无 output: " + response);
        }
        return parseDashScopeOutput(output);
    }

    private static AiImageGenerateResult parseDashScopeOutput(JSONObject output) {
        JSONArray results = output.getJSONArray("results");
        if (results != null && !results.isEmpty()) {
            JSONObject first = results.getJSONObject(0);
            if (StrUtil.isNotBlank(first.getStr("url"))) {
                return new AiImageGenerateResult().setUrl(first.getStr("url"));
            }
            if (StrUtil.isNotBlank(first.getStr("b64_json"))) {
                return new AiImageGenerateResult().setB64Json(first.getStr("b64_json"));
            }
        }

        JSONArray choices = output.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message != null) {
                JSONArray content = message.getJSONArray("content");
                if (content != null) {
                    for (int i = 0; i < content.size(); i++) {
                        JSONObject item = content.getJSONObject(i);
                        if (StrUtil.isNotBlank(item.getStr("image"))) {
                            return new AiImageGenerateResult().setUrl(item.getStr("image"));
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("[HttpAiImageClient] DashScope 响应无图像结果: " + output);
    }

    private static AiImageGenerateResult parseOpenAiCompatibleResponse(JSONObject json) {
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("[HttpAiImageClient] 图像响应无 data: " + json);
        }
        JSONObject first = data.getJSONObject(0);
        AiImageGenerateResult result = new AiImageGenerateResult();
        if (StrUtil.isNotBlank(first.getStr("b64_json"))) {
            result.setB64Json(first.getStr("b64_json"));
        }
        if (StrUtil.isNotBlank(first.getStr("url"))) {
            result.setUrl(first.getStr("url"));
        }
        if (StrUtil.isAllBlank(result.getB64Json(), result.getUrl())) {
            throw new IllegalStateException("[HttpAiImageClient] 图像响应 data[0] 无 b64_json/url: " + json);
        }
        return result;
    }

    private JSONObject postJson(String url, JSONObject body, Map<String, String> extraHeaders) {
        return postJson(url, body, extraHeaders, apiKey);
    }

    private JSONObject postJson(String url, JSONObject body, Map<String, String> extraHeaders, String bearerToken) {
        HttpRequest request = HttpRequest.post(url)
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(IMAGE_HTTP_TIMEOUT_MS);
        extraHeaders.forEach(request::header);
        HttpResponse response = request.execute();
        if (!response.isOk()) {
            throw new IllegalStateException(StrUtil.format(
                    "[HttpAiImageClient] 图像生成失败 platform={} status={} body={}",
                    platform.getPlatform(), response.getStatus(), response.body()));
        }
        return JSONUtil.parseObj(response.body());
    }

    private JSONObject getJson(String url) {
        HttpResponse response = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .timeout(IMAGE_HTTP_TIMEOUT_MS)
                .execute();
        if (!response.isOk()) {
            throw new IllegalStateException(StrUtil.format(
                    "[HttpAiImageClient] 图像任务查询失败 platform={} status={} body={}",
                    platform.getPlatform(), response.getStatus(), response.body()));
        }
        return JSONUtil.parseObj(response.body());
    }

    private void validatePlatform() {
        switch (platform) {
            case OPENAI, TONG_YI, ZHI_PU, SILICON_FLOW, STABLE_DIFFUSION, YI_YAN -> {
            }
            default -> throw new IllegalArgumentException("Unsupported image platform: " + platform);
        }
    }

    private static String getOption(Map<String, String> options, String... keys) {
        if (options == null) {
            return null;
        }
        for (String key : keys) {
            String value = options.get(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static void putIfNotBlank(JSONObject body, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            body.set(key, value);
        }
    }

    private static void putIfInteger(JSONObject body, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            body.set(key, Integer.parseInt(value));
        }
    }

    private static void putIfFloat(JSONObject body, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            body.set(key, Float.parseFloat(value));
        }
    }

    private static float parseFloatOption(Map<String, String> options, float defaultValue, String... keys) {
        String value = getOption(options, keys);
        return StrUtil.isBlank(value) ? defaultValue : Float.parseFloat(value);
    }

    private static int parseIntOption(Map<String, String> options, int defaultValue, String... keys) {
        String value = getOption(options, keys);
        return StrUtil.isBlank(value) ? defaultValue : Integer.parseInt(value);
    }

    private static String joinUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("[HttpAiImageClient] DashScope 任务轮询被中断", ex);
        }
    }

}
