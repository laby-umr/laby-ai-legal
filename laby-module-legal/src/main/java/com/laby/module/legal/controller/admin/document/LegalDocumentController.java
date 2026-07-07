package com.laby.module.legal.controller.admin.document;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.tenant.core.aop.TenantIgnore;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.legal.controller.admin.document.vo.LegalDocumentPreviewConfigRespVO;
import com.laby.module.legal.framework.config.LegalOnlyOfficeProperties;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;
import com.laby.module.legal.service.document.LegalDocumentPreviewService;
import com.laby.module.legal.service.document.LegalOnlyOfficeCallbackService;
import com.laby.module.legal.service.document.LegalOnlyOfficeFileTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.Set;

import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.module.infra.framework.file.core.utils.FileTypeUtils.writeAttachment;

/**
 * 合同文档预览（OnlyOffice）
 */
@Tag(name = "管理后台 - 法务合同文档预览")
@RestController
@RequestMapping("/legal/document")
@Validated
@Slf4j
public class LegalDocumentController {

    @Resource
    private LegalDocumentPreviewService previewService;
    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalOnlyOfficeProperties onlyOfficeProperties;
    @Resource
    private LegalOnlyOfficeFileTokenService fileTokenService;
    @Resource
    private LegalOnlyOfficeCallbackService onlyOfficeCallbackService;

    @GetMapping("/preview-config")
    @Operation(summary = "获取 OnlyOffice 编辑/预览配置")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalDocumentPreviewConfigRespVO> getPreviewConfig(
            @RequestParam("contractId") Long contractId) {
        return success(previewService.buildPreviewConfig(contractId));
    }

    @GetMapping({
            "/onlyoffice/file/{fileId}/{tenantId}/{exp}/{sig}",
            "/onlyoffice/file/{fileId}"
    })
    @Operation(summary = "OnlyOffice 拉取合同文件（path/query 短时令牌）")
    @PermitAll
    @TenantIgnore
    public void downloadForOnlyOffice(@PathVariable("fileId") Long fileId,
                                      @PathVariable(value = "tenantId", required = false) Long pathTenantId,
                                      @PathVariable(value = "exp", required = false) Long exp,
                                      @PathVariable(value = "sig", required = false) String sig,
                                      @RequestParam(value = "accessToken", required = false) String accessToken,
                                      HttpServletResponse response) throws Exception {
        Long tenantId = resolveOnlyOfficeTenantId(fileId, pathTenantId, exp, sig, accessToken);
        if (tenantId == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid access token");
            return;
        }
        TenantUtils.execute(tenantId, () -> {
            LegalContractFileDownloadBO download = contractService.downloadContractFile(fileId);
            writeAttachment(response, download.getFileName(), download.getContent());
            return null;
        });
    }

    private Long resolveOnlyOfficeTenantId(Long fileId, Long pathTenantId, Long exp, String sig, String accessToken) {
        if (pathTenantId != null && exp != null && sig != null) {
            return fileTokenService.validatePathToken(
                    fileId, pathTenantId, exp, sig, onlyOfficeProperties.getJwtSecret());
        }
        if (accessToken != null) {
            return fileTokenService.validateAndGetTenantId(
                    fileId, onlyOfficeProperties.getJwtSecret(), accessToken);
        }
        return null;
    }

    @PostMapping("/onlyoffice/callback")
    @Operation(summary = "OnlyOffice 编辑保存回调")
    @PermitAll
    @TenantIgnore
    public Map<String, Object> onlyOfficeCallback(@RequestBody Map<String, Object> body) {
        return onlyOfficeCallbackService.handleCallback(body);
    }

    @GetMapping("/onlyoffice/plugin/legal-locate-v2/vendor/{filename}")
    @Operation(summary = "OnlyOffice 定位插件 vendor 资源")
    @PermitAll
    @TenantIgnore
    public ResponseEntity<org.springframework.core.io.Resource> locatePluginVendorAsset(
            @PathVariable("filename") String filename) {
        if (!LOCATE_PLUGIN_VENDOR_FILES.contains(filename)) {
            return ResponseEntity.notFound().build();
        }
        org.springframework.core.io.Resource resource =
                new ClassPathResource("onlyoffice-plugin/legal-locate-v2/vendor/" + filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = "plugins.js".equals(filename)
                ? MediaType.valueOf("application/javascript")
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/onlyoffice/plugin/legal-locate-v2/{filename}")
    @Operation(summary = "OnlyOffice 定位插件静态资源（DS 拉取）")
    @PermitAll
    @TenantIgnore
    public ResponseEntity<?> locatePluginAsset(@PathVariable("filename") String filename) {
        if ("index.html".equals(filename)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildLocatePluginIndexHtml());
        }
        if ("config.json".equals(filename)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildLocatePluginConfigJson());
        }
        if (!LOCATE_PLUGIN_STATIC_FILES.contains(filename)) {
            return ResponseEntity.notFound().build();
        }
        org.springframework.core.io.Resource resource =
                new ClassPathResource("onlyoffice-plugin/legal-locate-v2/" + filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = "locate-core.js".equals(filename)
                ? MediaType.valueOf("application/javascript")
                : MediaType.TEXT_HTML;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(mediaType)
                .body(resource);
    }

    /**
     * plugins.js 同步加载后再执行 code.js，避免 Asc.plugin 竞态。
     */
    private String buildLocatePluginIndexHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <script type="text/javascript" src="vendor/plugins.js"></script>
                  <script type="text/javascript" src="locate-core.js?v=20260606-v3"></script>
                </head>
                <body></body>
                </html>
                """;
    }

    private String buildLocatePluginConfigJson() {
        String pluginBase = resolveLocatePluginBaseUrl();
        String baseUrl = pluginBase.endsWith("/") ? pluginBase : pluginBase + "/";
        return """
                {
                  "name": "LegalLocate",
                  "guid": "asc.legal-locate-v3",
                  "baseUrl": "%s",
                  "variations": [
                    {
                      "description": "法务审阅正文定位",
                      "url": "index.html",
                      "icons": [],
                      "isViewer": false,
                      "EditorsSupport": ["word"],
                      "isVisual": false,
                      "isModal": false,
                      "isInsideMode": false,
                      "isSystem": true,
                      "type": "system",
                      "initDataType": "none",
                      "initData": "",
                      "buttons": [],
                      "events": ["onDocumentReady"]
                    }
                  ]
                }
                """.formatted(baseUrl.replace("\\", "\\\\").replace("\"", "\\\""));
    }

    private String resolveLocatePluginBaseUrl() {
        String apiRoot;
        if (StrUtil.isNotBlank(onlyOfficeProperties.getPluginBaseUrl())) {
            apiRoot = StrUtil.removeSuffix(onlyOfficeProperties.getPluginBaseUrl(), "/");
        } else {
            apiRoot = StrUtil.removeSuffix(
                    StrUtil.blankToDefault(onlyOfficeProperties.getCallbackBaseUrl(), ""), "/")
                    .replace("host.docker.internal", "127.0.0.1");
        }
        return apiRoot + "/legal/document/onlyoffice/plugin/legal-locate-v2";
    }

    private static final Set<String> LOCATE_PLUGIN_STATIC_FILES = Set.of("locate-core.js");

    private static final Set<String> LOCATE_PLUGIN_VENDOR_FILES = Set.of("plugins.js");
}
