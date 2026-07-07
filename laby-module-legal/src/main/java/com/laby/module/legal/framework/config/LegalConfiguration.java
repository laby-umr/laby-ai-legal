package com.laby.module.legal.framework.config;

import com.laby.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import com.laby.module.legal.framework.ratelimiter.TenantRateLimiterKeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
public class LegalConfiguration {

    @Bean
    public RateLimiterKeyResolver tenantRateLimiterKeyResolver() {
        return new TenantRateLimiterKeyResolver();
    }

}
