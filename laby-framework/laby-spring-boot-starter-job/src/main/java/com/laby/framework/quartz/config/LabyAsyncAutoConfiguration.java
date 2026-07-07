package com.laby.framework.quartz.config;

import com.alibaba.ttl.TtlRunnable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务 Configuration：有界线程池 + TTL 上下文透传。
 */
@AutoConfiguration(before = TaskExecutionAutoConfiguration.class)
@EnableAsync
public class LabyAsyncAutoConfiguration implements AsyncConfigurer {

    private final ObjectProvider<ThreadPoolTaskExecutor> applicationTaskExecutorProvider;

    public LabyAsyncAutoConfiguration(ObjectProvider<ThreadPoolTaskExecutor> applicationTaskExecutorProvider) {
        this.applicationTaskExecutorProvider = applicationTaskExecutorProvider;
    }

    @Bean(name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    @ConditionalOnMissingBean(name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        ThreadPoolTaskExecutor executor = builder.build();
        executor.setTaskDecorator(TtlRunnable::get);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = applicationTaskExecutorProvider.getIfAvailable();
        return executor != null ? executor : new SimpleAsyncTaskExecutor();
    }

    @Bean
    public BeanPostProcessor threadPoolTaskExecutorBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            @SuppressWarnings("PatternVariableCanBeUsed")
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof ThreadPoolTaskExecutor executor) {
                    executor.setTaskDecorator(TtlRunnable::get);
                    return executor;
                }
                if (bean instanceof SimpleAsyncTaskExecutor executor) {
                    executor.setTaskDecorator(TtlRunnable::get);
                    return executor;
                }
                return bean;
            }

        };
    }

}
