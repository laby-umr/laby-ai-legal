package com.laby.module.ai.util;

import com.laby.framework.common.util.collection.SetUtils;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.framework.tenant.core.context.TenantContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AI 工具类
 *
 * @author 芋道源码
 */
public class AiUtils {

    public static final String TOOL_CONTEXT_LOGIN_USER = "LOGIN_USER";
    public static final String TOOL_CONTEXT_TENANT_ID = "TENANT_ID";

    /**
     * 通义千问支持多模态的模型
     *
     * @see <a href="https://bailian.console.aliyun.com/cn-beijing/?tab=model#/model-market/all?providers=qwen&capabilities=VU">模型广场</a>
     * @see <a href="https://help.aliyun.com/zh/model-studio/error-code#error-url">必须开启 withMultiModel 参数</a>
     */
    public static final Set<String> TONG_YI_MULTI_MODELS = SetUtils.asSet(
            // qwen3.5 / 3.6 系列（统一多模态主干）
            "qwen3.6-plus", "qwen3.6-flash",
            "qwen3.5-plus", "qwen3.5-flash",
            // qwen-vl 视觉理解
            "qwen3-vl-plus", "qwen3-vl-flash",
            "qwen-vl-max", "qwen-vl-plus",
            "qwen2.5-vl-72b-instruct", "qwen2.5-vl-32b-instruct",
            "qwen2.5-vl-7b-instruct", "qwen2.5-vl-3b-instruct",
            // qvq 视觉推理
            "qvq-max", "qvq-plus",
            // qwen-omni 全模态
            "qwen3.5-omni-plus", "qwen3.5-omni-flash",
            "qwen3-omni-flash", "qwen-omni-turbo"
    );

    public static Map<String, Object> buildCommonToolContext() {
        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CONTEXT_LOGIN_USER, SecurityFrameworkUtils.getLoginUser());
        context.put(TOOL_CONTEXT_TENANT_ID, TenantContextHolder.getTenantId());
        return context;
    }

}
