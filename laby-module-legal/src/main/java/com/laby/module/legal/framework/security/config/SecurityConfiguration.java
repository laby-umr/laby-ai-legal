package com.laby.module.legal.framework.security.config;

import com.laby.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * 法务模块 Security 配置
 */
@Configuration(proxyBeanMethods = false, value = "legalSecurityConfiguration")
public class SecurityConfiguration {

    @Bean("legalAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(
                    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                registry.requestMatchers(buildAdminApi("/legal/document/onlyoffice/**")).permitAll();
            }
        };
    }

}
