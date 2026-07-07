package com.laby.module.legal.framework.onlyoffice;

import com.laby.framework.common.enums.WebFilterOrderEnum;
import com.laby.framework.tenant.config.TenantProperties;
import com.laby.framework.web.config.WebProperties;
import com.laby.module.legal.framework.config.LegalOnlyOfficeProperties;
import com.laby.module.legal.service.document.LegalOnlyOfficeFileTokenService;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OnlyOffice 集成 Web 过滤器与租户白名单
 */
@Configuration(proxyBeanMethods = false)
public class LegalOnlyOfficeWebConfiguration {

    private final TenantProperties tenantProperties;
    private final WebProperties webProperties;

    public LegalOnlyOfficeWebConfiguration(TenantProperties tenantProperties, WebProperties webProperties) {
        this.tenantProperties = tenantProperties;
        this.webProperties = webProperties;
    }

    /**
     * 运行时注册租户忽略 URL（与 application.yaml 双保险，避免 @TenantIgnore 扫描路径无前缀导致不匹配）。
     */
    @PostConstruct
    public void registerTenantIgnoreUrls() {
        String prefix = webProperties.getAdminApi().getPrefix();
        tenantProperties.getIgnoreUrls().add(prefix + "/legal/document/onlyoffice/**");
    }

    @Bean
    public FilterRegistrationBean<LegalOnlyOfficeTenantWebFilter> legalOnlyOfficeTenantWebFilter(
            LegalOnlyOfficeProperties onlyOfficeProperties,
            LegalOnlyOfficeFileTokenService fileTokenService) {
        FilterRegistrationBean<LegalOnlyOfficeTenantWebFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new LegalOnlyOfficeTenantWebFilter(
                webProperties, onlyOfficeProperties, fileTokenService));
        // 紧贴 TenantSecurityWebFilter(-99) 之前执行，确保 path 令牌已解析
        bean.setOrder(WebFilterOrderEnum.TENANT_SECURITY_FILTER - 1);
        return bean;
    }
}
