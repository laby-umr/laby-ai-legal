package com.laby.module.legal.service.document;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.legal.service.contract.LegalContractVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OnlyOffice 编辑保存回调
 */
@Slf4j
@Service
public class LegalOnlyOfficeCallbackService {

    @Resource
    private LegalContractVersionService contractVersionService;

    /**
     * @see <a href="https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/">Callback handler</a>
     */
    public Map<String, Object> handleCallback(Map<String, Object> body) {
        int status = MapUtil.getInt(body, "status", -1);
        // 2 = MustSave, 6 = MustSaveForce
        if (status != 2 && status != 6) {
            return success();
        }
        String key = MapUtil.getStr(body, "key");
        String url = MapUtil.getStr(body, "url");
        LegalOnlyOfficeDocumentKeyHelper.Parsed parsed = LegalOnlyOfficeDocumentKeyHelper.parse(key);
        if (parsed == null || StrUtil.isBlank(url)) {
            log.warn("[onlyOfficeCallback][invalid] key={} urlBlank={}", key, StrUtil.isBlank(url));
            return error();
        }
        try {
            byte[] content = HttpUtil.downloadBytes(url);
            if (content == null || content.length == 0) {
                log.warn("[onlyOfficeCallback][empty] contractId={} fileId={}", parsed.getContractId(),
                        parsed.getFileId());
                return error();
            }
            TenantUtils.execute(parsed.getTenantId(), () -> {
                contractVersionService.saveWorkingFromOnlyOffice(
                        parsed.getContractId(), parsed.getFileId(), content);
                return null;
            });
            log.info("[onlyOfficeCallback][saved] contractId={} fileId={} bytes={}",
                    parsed.getContractId(), parsed.getFileId(), content.length);
            return success();
        } catch (Exception ex) {
            log.error("[onlyOfficeCallback][failed] key={}", key, ex);
            return error();
        }
    }

    private static Map<String, Object> success() {
        return Map.of("error", 0);
    }

    private static Map<String, Object> error() {
        return Map.of("error", 1);
    }
}
