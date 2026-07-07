package com.laby.module.bpm.framework.web.config;

import com.laby.framework.common.enums.WebFilterOrderEnum;
import com.laby.framework.swagger.config.LabySwaggerAutoConfiguration;
import com.laby.module.bpm.framework.web.core.FlowableWebFilter;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bpm 模块的 web 组件的 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class BpmWebConfiguration {

    /**
     * bpm 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi bpmGroupedOpenApi() {
        return LabySwaggerAutoConfiguration.buildGroupedOpenApi("bpm");
    }

    /**
     * 配置 Flowable Web 过滤器
     */
    @Bean
    public FilterRegistrationBean<FlowableWebFilter> flowableWebFilter() {
        FilterRegistrationBean<FlowableWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new FlowableWebFilter());
        registrationBean.setOrder(WebFilterOrderEnum.FLOWABLE_FILTER);
        return registrationBean;
    }

}
