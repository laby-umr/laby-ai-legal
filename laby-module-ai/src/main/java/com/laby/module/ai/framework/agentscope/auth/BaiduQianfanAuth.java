package com.laby.module.ai.framework.agentscope.auth;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;

/**
 * 百度千帆鉴权：支持新版 API Key（Bearer）与旧版 appKey|secretKey（OAuth access_token）
 */
public final class BaiduQianfanAuth {

    private static final String OAUTH_URL = "https://aip.baidubce.com/oauth/2.0/token";

    private BaiduQianfanAuth() {
    }

    public static String resolveBearerToken(String apiKey) {
        if (StrUtil.isBlank(apiKey) || !apiKey.contains("|")) {
            return apiKey;
        }
        List<String> keys = StrUtil.split(apiKey, '|');
        if (keys.size() != 2) {
            throw new IllegalArgumentException("文心一言密钥需为 appKey|secretKey 或新版 API Key");
        }
        return fetchAccessToken(keys.get(0), keys.get(1));
    }

    private static String fetchAccessToken(String appKey, String secretKey) {
        String url = StrUtil.format(
                "{}?grant_type=client_credentials&client_id={}&client_secret={}",
                OAUTH_URL, appKey, secretKey);
        HttpResponse response = HttpRequest.get(url).timeout(30_000).execute();
        if (!response.isOk()) {
            throw new IllegalStateException(StrUtil.format(
                    "[BaiduQianfanAuth] 获取 access_token 失败 status={} body={}",
                    response.getStatus(), response.body()));
        }
        JSONObject json = JSONUtil.parseObj(response.body());
        String token = json.getStr("access_token");
        if (StrUtil.isBlank(token)) {
            throw new IllegalStateException("[BaiduQianfanAuth] OAuth 响应无 access_token: " + response.body());
        }
        return token;
    }

}
