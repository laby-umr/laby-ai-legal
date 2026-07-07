package com.laby.module.legal.service.document;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * OnlyOffice 拉取合同文件的短时令牌（query accessToken）
 */
@Component
public class LegalOnlyOfficeFileTokenService {

    private static final String SEP = ":";

    public String createToken(Long fileId, Long tenantId, String secret, int ttlMinutes) {
        long exp = Instant.now().getEpochSecond() + ttlMinutes * 60L;
        String raw = fileId + SEP + tenantId + SEP + exp;
        String sig = hmacSha256Hex(secret, raw);
        return tenantId + SEP + exp + SEP + sig;
    }

    /**
     * 路径式拉流 URL（OnlyOffice DS 常会丢弃 query 参数，令牌必须放在 path 中）。
     */
    public String buildFileDownloadUrl(Long fileId, Long tenantId, String apiBase, String secret, int ttlMinutes) {
        String accessToken = createToken(fileId, tenantId, secret, ttlMinutes);
        String[] parts = accessToken.split(SEP, 3);
        if (parts.length != 3) {
            throw new IllegalStateException("OnlyOffice accessToken 格式异常");
        }
        return apiBase + "/legal/document/onlyoffice/file/" + fileId + "/" + parts[0] + "/" + parts[1] + "/" + parts[2];
    }

    public Long validatePathToken(Long fileId, Long tenantId, Long exp, String sig, String secret) {
        if (fileId == null || tenantId == null || exp == null || StrUtil.isBlank(sig)) {
            return null;
        }
        return validateAndGetTenantId(fileId, secret, tenantId + SEP + exp + SEP + sig);
    }

    /**
     * 校验令牌并返回租户编号（供 PermitAll 回调设置租户上下文）
     */
    public Long validateAndGetTenantId(Long fileId, String secret, String accessToken) {
        if (StrUtil.isBlank(accessToken) || StrUtil.isBlank(secret)) {
            return null;
        }
        String[] parts = accessToken.split(SEP, 3);
        if (parts.length != 3) {
            return null;
        }
        long tenantId;
        long exp;
        try {
            tenantId = Long.parseLong(parts[0]);
            exp = Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (Instant.now().getEpochSecond() > exp) {
            return null;
        }
        String raw = fileId + SEP + tenantId + SEP + exp;
        String expected = hmacSha256Hex(secret, raw);
        if (!expected.equals(parts[2])) {
            return null;
        }
        return tenantId;
    }

    private static String hmacSha256Hex(String secret, String raw) {
        return new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes()).digestHex(raw);
    }
}
