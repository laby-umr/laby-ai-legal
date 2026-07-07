package com.laby.module.ai.framework.agentscope.model;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.framework.agentscope.auth.BaiduQianfanAuth;
import com.laby.module.ai.framework.ai.core.model.siliconflow.SiliconFlowApiConstants;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.formatter.openai.GLMFormatter;
import io.agentscope.core.formatter.openai.dto.OpenAIMessage;
import io.agentscope.core.formatter.openai.dto.OpenAIRequest;
import io.agentscope.core.formatter.openai.dto.OpenAIResponse;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GeminiChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.transport.OkHttpTransport;

import java.util.List;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.ai.enums.ErrorCodeConstants.MODEL_NOT_EXISTS;

public final class AgentScopeModelFactory {

    private static final String GROK_BASE_URL = "https://api.x.ai";
    private static final String GROK_CHAT_PATH = "/v1/chat/completions";
    private static final String DOU_BAO_BASE_URL = "https://ark.cn-beijing.volces.com/api";
    private static final String DOU_BAO_CHAT_PATH = "/v3/chat/completions";
    private static final String HUN_YUAN_BASE_URL = "https://api.hunyuan.cloud.tencent.com";
    private static final String HUN_YUAN_DEEPSEEK_BASE_URL = "https://api.lkeap.cloud.tencent.com";
    private static final String HUN_YUAN_CHAT_PATH = "/v1/chat/completions";
    private static final String XING_HUO_BASE_URL = "https://spark-api-open.xf-yun.com";
    private static final String XING_HUO_CHAT_PATH_V2 = "/v2/chat/completions";
    private static final String BAI_CHUAN_BASE_URL = "https://api.baichuan-ai.com/v1";
    private static final String QIANFAN_BASE_URL = "https://qianfan.baidubce.com/v2";
    private static final String MOONSHOT_BASE_URL = "https://api.moonshot.cn/v1";
    private static final String MINI_MAX_BASE_URL = "https://api.minimax.chat/v1";

    private AgentScopeModelFactory() {}

    public static AgentScopeModelConfig from(AiModelDO model, AiApiKeyDO apiKey) {
        return AgentScopeModelConfig.builder()
                .platform(AiPlatformEnum.validatePlatform(apiKey.getPlatform()))
                .modelName(model.getModel())
                .apiKey(apiKey.getApiKey())
                .baseUrl(apiKey.getUrl())
                .temperature(model.getTemperature())
                .maxTokens(model.getMaxTokens())
                .build();
    }

    /**
     * 构造 AgentScope Model 实例（OpenAI 兼容 / DashScope / Gemini / Anthropic）
     */
    public static Model buildChatModel(AgentScopeModelConfig config) {
        return buildChatModel(config, null, 2);
    }

    public static Model buildChatModel(AgentScopeModelConfig config, int modelMaxRetries) {
        return buildChatModel(config, null, modelMaxRetries);
    }

    public static Model buildChatModel(AgentScopeModelConfig config, AiLlmRequest request) {
        return buildChatModel(config, request, 2);
    }

    public static Model buildChatModel(AgentScopeModelConfig config, AiLlmRequest request, int modelMaxRetries) {
        GenerateOptions options = buildGenerateOptions(config, request, modelMaxRetries);
        AiPlatformEnum platform = config.getPlatform();
        return switch (platform) {
            case TONG_YI -> DashScopeChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .defaultOptions(options)
                    .build();
            case GEMINI -> GeminiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .defaultOptions(options)
                    .build();
            case ANTHROPIC -> AnthropicChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(StrUtil.blankToDefault(config.getBaseUrl(), "https://api.anthropic.com"))
                    .modelName(config.getModelName())
                    .defaultOptions(options)
                    .build();
            case OPENAI, SILICON_FLOW, AZURE_OPENAI, MOONSHOT, BAI_CHUAN ->
                    buildOpenAiChatModel(config, resolveOpenAiCompatibleBaseUrl(platform, config.getBaseUrl()),
                            null, options, null);
            case DEEP_SEEK ->
                    buildOpenAiChatModel(config, resolveOpenAiCompatibleBaseUrl(platform, config.getBaseUrl()),
                            null, options, new DeepSeekFormatter());
            case ZHI_PU ->
                    buildOpenAiChatModel(config, resolveOpenAiCompatibleBaseUrl(platform, config.getBaseUrl()),
                            "/chat/completions", options, new GLMFormatter());
            case DOU_BAO -> buildOpenAiChatModel(config,
                    StrUtil.blankToDefault(config.getBaseUrl(), DOU_BAO_BASE_URL), DOU_BAO_CHAT_PATH, options, null);
            case HUN_YUAN -> buildOpenAiChatModel(config,
                    resolveHunYuanBaseUrl(config.getBaseUrl(), config.getModelName()), HUN_YUAN_CHAT_PATH, options, null);
            case MINI_MAX -> buildOpenAiChatModel(config,
                    StrUtil.blankToDefault(config.getBaseUrl(), MINI_MAX_BASE_URL), null, options, null);
            case GROK -> buildOpenAiChatModel(config,
                    StrUtil.blankToDefault(config.getBaseUrl(), GROK_BASE_URL), GROK_CHAT_PATH, options, null);
            case XING_HUO -> buildXingHuoChatModel(config, options);
            case YI_YAN -> buildOpenAiChatModel(config,
                    BaiduQianfanAuth.resolveBearerToken(config.getApiKey()),
                    StrUtil.blankToDefault(config.getBaseUrl(), QIANFAN_BASE_URL), null, options, null);
            case OLLAMA -> OpenAIChatModel.builder()
                    .apiKey("ollama")
                    .baseUrl(normalizeOllamaBaseUrl(config.getBaseUrl()))
                    .modelName(config.getModelName())
                    .generateOptions(options)
                    .httpTransport(new OkHttpTransport())
                    .build();
            default -> throw exception(MODEL_NOT_EXISTS);
        };
    }

    /** 供 HarnessAgent.builder().model("dashscope:qwen-plus") 使用的字符串形式 */
    public static String toModelRef(AgentScopeModelConfig config) {
        return switch (config.getPlatform()) {
            case TONG_YI -> "dashscope:" + config.getModelName();
            case OPENAI -> "openai:" + config.getModelName();
            case DEEP_SEEK -> "openai:" + config.getModelName();
            case OLLAMA -> "ollama:" + config.getModelName();
            case GEMINI -> "gemini:" + config.getModelName();
            case ANTHROPIC -> "anthropic:" + config.getModelName();
            default -> null;
        };
    }

    private static Model buildOpenAiChatModel(AgentScopeModelConfig config, String baseUrl, String endpointPath,
                                              GenerateOptions options,
                                              Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter) {
        return buildOpenAiChatModel(config, config.getApiKey(), baseUrl, endpointPath, options, formatter);
    }

    private static Model buildOpenAiChatModel(AgentScopeModelConfig config, String apiKey,
                                            String baseUrl, String endpointPath, GenerateOptions options,
                                            Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter) {
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(config.getModelName())
                .generateOptions(options)
                .httpTransport(new OkHttpTransport());
        if (formatter != null) {
            builder.formatter(formatter);
        }
        if (StrUtil.isNotBlank(endpointPath)) {
            builder.endpointPath(endpointPath);
        }
        return builder.build();
    }

    private static Model buildXingHuoChatModel(AgentScopeModelConfig config, GenerateOptions options) {
        List<String> keys = StrUtil.split(config.getApiKey(), '|');
        Assert.equals(keys.size(), 2, "星火密钥需为 appKey|secretKey 格式");
        String apiKey = keys.get(0) + ":" + keys.get(1);
        boolean useV2 = "x1".equalsIgnoreCase(config.getModelName());
        String baseUrl = StrUtil.blankToDefault(config.getBaseUrl(), XING_HUO_BASE_URL);
        String endpointPath = useV2 ? XING_HUO_CHAT_PATH_V2 : null;
        return buildOpenAiChatModel(config, apiKey, baseUrl, endpointPath, options, null);
    }

    private static GenerateOptions buildGenerateOptions(AgentScopeModelConfig config, AiLlmRequest request,
                                                        int modelMaxRetries) {
        GenerateOptions.Builder builder = GenerateOptions.builder();
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        Integer maxTokens = config.getMaxTokens();
        if (request != null) {
            if (request.getMaxTokens() != null && request.getMaxTokens() > 0) {
                maxTokens = request.getMaxTokens();
            }
            if (request.getTemperature() != null) {
                builder.temperature(request.getTemperature());
            }
            if (request.isJsonMode()) {
                builder.additionalBodyParam("response_format", Map.of("type", "json_object"));
            }
        }
        if (maxTokens != null && maxTokens > 0) {
            builder.maxTokens(maxTokens);
        }
        if (modelMaxRetries > 0) {
            builder.executionConfig(ExecutionConfig.builder()
                    .maxAttempts(1 + modelMaxRetries)
                    .build());
        }
        return builder.build();
    }

    private static String resolveOpenAiCompatibleBaseUrl(AiPlatformEnum platform, String url) {
        if (StrUtil.isBlank(url)) {
            return switch (platform) {
                case DEEP_SEEK -> "https://api.deepseek.com/v1";
                case ZHI_PU -> "https://open.bigmodel.cn/api/paas/v4";
                case MOONSHOT -> MOONSHOT_BASE_URL;
                case BAI_CHUAN -> BAI_CHUAN_BASE_URL;
                case SILICON_FLOW -> SiliconFlowApiConstants.DEFAULT_BASE_URL + "/v1";
                default -> "https://api.openai.com/v1";
            };
        }
        String normalized = url.trim().replaceAll("/+$", "");
        if (platform == AiPlatformEnum.DEEP_SEEK && !normalized.endsWith("/v1")) {
            return normalized + "/v1";
        }
        return normalized;
    }

    private static String resolveHunYuanBaseUrl(String url, String modelName) {
        if (StrUtil.isNotBlank(url)) {
            return url;
        }
        return StrUtil.startWithIgnoreCase(modelName, "deepseek")
                ? HUN_YUAN_DEEPSEEK_BASE_URL
                : HUN_YUAN_BASE_URL;
    }

    private static String normalizeOllamaBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:11434/v1";
        }
        return url.endsWith("/v1") ? url : url.replaceAll("/+$", "") + "/v1";
    }

}
