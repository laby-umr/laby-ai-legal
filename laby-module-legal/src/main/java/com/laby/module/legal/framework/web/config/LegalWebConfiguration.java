package com.laby.module.legal.framework.web.config;

import com.laby.framework.swagger.config.LabySwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LegalWebConfiguration {

    @Bean
    public GroupedOpenApi legalGroupedOpenApi() {
        return LabySwaggerAutoConfiguration.buildGroupedOpenApi("legal");
    }

}
