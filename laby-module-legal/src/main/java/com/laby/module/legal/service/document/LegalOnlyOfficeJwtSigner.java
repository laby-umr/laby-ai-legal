package com.laby.module.legal.service.document;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSignerUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OnlyOffice 文档配置 JWT 签名（HS256）
 */
@Component
public class LegalOnlyOfficeJwtSigner {

    public String sign(Map<String, Object> payload, String secret) {
        if (StrUtil.isBlank(secret)) {
            return null;
        }
        JSONObject json = new JSONObject(payload);
        return JWT.create()
                .addPayloads(json)
                .setSigner(JWTSignerUtil.hs256(secret.getBytes(StandardCharsets.UTF_8)))
                .sign();
    }
}
