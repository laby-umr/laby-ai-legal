package com.laby.module.ai.framework.agentscope.chat;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.agentsflex.core.llm.Llm;
import com.agentsflex.llm.deepseek.DeepseekConfig;
import com.agentsflex.llm.deepseek.DeepseekLlm;
import com.agentsflex.llm.ollama.OllamaLlm;
import com.agentsflex.llm.ollama.OllamaLlmConfig;
import com.agentsflex.llm.openai.OpenAILlm;
import com.agentsflex.llm.openai.OpenAILlmConfig;
import com.agentsflex.llm.qianfan.QianFanLlm;
import com.agentsflex.llm.qianfan.QianFanLlmConfig;
import com.agentsflex.llm.qwen.QwenLlm;
import com.agentsflex.llm.qwen.QwenLlmConfig;
import com.agentsflex.llm.siliconflow.SiliconflowConfig;
import com.agentsflex.llm.siliconflow.SiliconflowLlm;
import com.agentsflex.llm.spark.SparkLlm;
import com.agentsflex.llm.spark.SparkLlmConfig;
import com.agentsflex.llm.tencent.TencentLlmConfig;
import com.agentsflex.llm.tencent.TencentlmLlm;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.module.ai.framework.agentscope.auth.BaiduQianfanAuth;
import com.laby.module.ai.framework.ai.core.model.siliconflow.SiliconFlowApiConstants;

import dev.tinyflow.core.provider.LlmProvider;

import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.ai.enums.ErrorCodeConstants.MODEL_NOT_EXISTS;

/**
 * 为 TinyFlow 工作流节点提供 agents-flex {@link Llm} 实现
 */
public final class TinyflowLlmProviderFactory {

    private TinyflowLlmProviderFactory() {
    }

    public static LlmProvider build(AiModelDO model, AiApiKeyDO apiKey) {
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(apiKey.getPlatform());
        return id -> createLlm(platform, model, apiKey);
    }

    private static Llm createLlm(AiPlatformEnum platform, AiModelDO model, AiApiKeyDO apiKey) {
        switch (platform) {
            case TONG_YI:
                QwenLlmConfig qwenConfig = new QwenLlmConfig();
                qwenConfig.setApiKey(apiKey.getApiKey());
                qwenConfig.setModel(model.getModel());
                return new QwenLlm(qwenConfig);
            case OLLAMA:
                OllamaLlmConfig ollamaConfig = new OllamaLlmConfig();
                ollamaConfig.setEndpoint(apiKey.getUrl());
                ollamaConfig.setModel(model.getModel());
                return new OllamaLlm(ollamaConfig);
            case OPENAI:
                return new OpenAILlm(openAiConfig(apiKey.getApiKey(), model.getModel(),
                        StrUtil.blankToDefault(apiKey.getUrl(), "https://api.openai.com/v1"), null));
            case DEEP_SEEK:
                return new DeepseekLlm(deepseekConfig(apiKey.getApiKey(), model.getModel(),
                        StrUtil.blankToDefault(apiKey.getUrl(), "https://api.deepseek.com")));
            case ZHI_PU:
                return new OpenAILlm(openAiConfig(apiKey.getApiKey(), model.getModel(),
                        StrUtil.blankToDefault(apiKey.getUrl(), "https://open.bigmodel.cn/api/paas/v4"), null));
            case MOONSHOT:
                return new OpenAILlm(openAiConfig(apiKey.getApiKey(), model.getModel(),
                        StrUtil.blankToDefault(apiKey.getUrl(), "https://api.moonshot.cn/v1"), null));
            case SILICON_FLOW:
                SiliconflowConfig siliconflowConfig = new SiliconflowConfig(apiKey.getApiKey());
                siliconflowConfig.setModel(model.getModel());
                siliconflowConfig.setEndpoint(StrUtil.blankToDefault(
                        apiKey.getUrl(), SiliconFlowApiConstants.DEFAULT_BASE_URL));
                return new SiliconflowLlm(siliconflowConfig);
            case DOU_BAO:
                return new OpenAILlm(openAiConfig(apiKey.getApiKey(), model.getModel(),
                        StrUtil.blankToDefault(apiKey.getUrl(), "https://ark.cn-beijing.volces.com/api"),
                        "/v3/chat/completions"));
            case HUN_YUAN:
                return new TencentlmLlm(tencentConfig(apiKey.getApiKey(), model.getModel(),
                        resolveHunYuanBaseUrl(apiKey.getUrl(), model.getModel())));
            case YI_YAN:
                QianFanLlmConfig qianFanConfig = new QianFanLlmConfig(
                        BaiduQianfanAuth.resolveBearerToken(apiKey.getApiKey()));
                qianFanConfig.setModel(model.getModel());
                if (StrUtil.isNotBlank(apiKey.getUrl())) {
                    qianFanConfig.setEndpoint(apiKey.getUrl());
                }
                return new QianFanLlm(qianFanConfig);
            case XING_HUO:
                return new SparkLlm(sparkConfig(apiKey.getApiKey(), model.getModel()));
            case BAI_CHUAN:
                return new OpenAILlm(openAiConfig(apiKey.getApiKey(), model.getModel(),
                        StrUtil.blankToDefault(apiKey.getUrl(), "https://api.baichuan-ai.com/v1"), null));
            case GROK:
                return new OpenAILlm(openAiConfig(apiKey.getApiKey(), model.getModel(),
                        StrUtil.blankToDefault(apiKey.getUrl(), "https://api.x.ai"), "/v1/chat/completions"));
            default:
                throw exception(MODEL_NOT_EXISTS);
        }
    }

    private static OpenAILlmConfig openAiConfig(String apiKey, String model, String endpoint, String chatPath) {
        OpenAILlmConfig config = new OpenAILlmConfig();
        config.setApiKey(apiKey);
        config.setModel(model);
        config.setEndpoint(endpoint);
        if (StrUtil.isNotBlank(chatPath)) {
            config.setChatPath(chatPath);
        }
        return config;
    }

    private static DeepseekConfig deepseekConfig(String apiKey, String model, String endpoint) {
        DeepseekConfig config = new DeepseekConfig();
        config.setApiKey(apiKey);
        config.setModel(model);
        config.setEndpoint(endpoint);
        return config;
    }

    private static TencentLlmConfig tencentConfig(String apiKey, String model, String endpoint) {
        TencentLlmConfig config = new TencentLlmConfig();
        config.setApiKey(apiKey);
        config.setModel(model);
        config.setEndpoint(endpoint);
        return config;
    }

    private static SparkLlmConfig sparkConfig(String apiKey, String model) {
        List<String> keys = StrUtil.split(apiKey, '|');
        Assert.equals(keys.size(), 2, "星火密钥需为 appKey|secretKey 格式");
        SparkLlmConfig config = new SparkLlmConfig();
        config.setApiKey(keys.get(0));
        config.setApiSecret(keys.get(1));
        config.setModel(model);
        return config;
    }

    private static String resolveHunYuanBaseUrl(String url, String modelName) {
        if (StrUtil.isNotBlank(url)) {
            return url;
        }
        return StrUtil.startWithIgnoreCase(modelName, "deepseek")
                ? "https://api.lkeap.cloud.tencent.com"
                : "https://api.hunyuan.cloud.tencent.com";
    }

}
