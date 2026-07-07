package com.laby.module.legal.framework.onlyoffice;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.framework.web.config.WebProperties;
import com.laby.module.legal.framework.config.LegalOnlyOfficeProperties;
import com.laby.module.legal.service.document.LegalOnlyOfficeFileTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * OnlyOffice DS 拉流/回调不带 tenant-id 头：在租户安全过滤器之前解析 path/query 令牌并设置租户。
 */
@Slf4j
@RequiredArgsConstructor
public class LegalOnlyOfficeTenantWebFilter extends OncePerRequestFilter {

    private static final String ONLYOFFICE_PATH = "/legal/document/onlyoffice/";

    private final WebProperties webProperties;
    private final LegalOnlyOfficeProperties onlyOfficeProperties;
    private final LegalOnlyOfficeFileTokenService fileTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String apiUri = request.getRequestURI().substring(request.getContextPath().length());
        String prefix = webProperties.getAdminApi().getPrefix();
        if (!apiUri.startsWith(prefix)) {
            return true;
        }
        String path = apiUri.substring(prefix.length());
        return !path.contains(ONLYOFFICE_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().contains("/onlyoffice/callback")) {
            TenantContextHolder.setIgnore(true);
        } else if (request.getRequestURI().contains("/onlyoffice/file/")) {
            applyTenantFromFileRequest(request);
        }
        chain.doFilter(request, response);
    }

    private void applyTenantFromFileRequest(HttpServletRequest request) {
        PathFileToken pathToken = parsePathFileToken(request.getRequestURI());
        if (pathToken != null) {
            Long tenantId = fileTokenService.validatePathToken(
                    pathToken.fileId(),
                    pathToken.tenantId(),
                    pathToken.exp(),
                    pathToken.sig(),
                    onlyOfficeProperties.getJwtSecret());
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
                return;
            }
            log.warn("[OnlyOfficeTenantFilter][uri={}] path 令牌校验失败", request.getRequestURI());
        }
        String accessToken = request.getParameter("accessToken");
        if (StrUtil.isNotBlank(accessToken)) {
            Long fileId = parseQueryFileId(request.getRequestURI());
            if (fileId != null) {
                Long tenantId = fileTokenService.validateAndGetTenantId(
                        fileId, onlyOfficeProperties.getJwtSecret(), accessToken);
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                    return;
                }
            }
            log.warn("[OnlyOfficeTenantFilter][uri={}] query 令牌校验失败", request.getRequestURI());
        }
        // 拉流接口在 ignore-urls 中，无令牌时由 TenantSecurity 放行后 Controller 返回 403
        TenantContextHolder.setIgnore(true);
    }

    private static PathFileToken parsePathFileToken(String uri) {
        int idx = uri.lastIndexOf("/onlyoffice/file/");
        if (idx < 0) {
            return null;
        }
        String tail = uri.substring(idx + "/onlyoffice/file/".length());
        int q = tail.indexOf('?');
        if (q >= 0) {
            tail = tail.substring(0, q);
        }
        String[] parts = tail.split("/");
        if (parts.length < 4) {
            return null;
        }
        try {
            return new PathFileToken(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1]),
                    Long.parseLong(parts[2]),
                    parts[3]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long parseQueryFileId(String uri) {
        int idx = uri.lastIndexOf("/onlyoffice/file/");
        if (idx < 0) {
            return null;
        }
        String tail = uri.substring(idx + "/onlyoffice/file/".length());
        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }
        int q = tail.indexOf('?');
        if (q >= 0) {
            tail = tail.substring(0, q);
        }
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record PathFileToken(Long fileId, Long tenantId, Long exp, String sig) {
    }

}
