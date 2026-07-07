package com.laby.framework.idempotent.config;

import com.laby.framework.idempotent.core.aop.IdempotentAspect;
import com.laby.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import com.laby.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import com.laby.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import com.laby.framework.idempotent.core.keyresolver.impl.UserIdempotentKeyResolver;
import com.laby.framework.idempotent.core.redis.IdempotentRedisDAO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import com.laby.framework.redis.config.LabyRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@AutoConfiguration(after = LabyRedisAutoConfiguration.class)
public class LabyIdempotentConfiguration {

    @Bean
    public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO) {
        return new IdempotentAspect(keyResolvers, idempotentRedisDAO);
    }

    @Bean
    public IdempotentRedisDAO idempotentRedisDAO(StringRedisTemplate stringRedisTemplate) {
        return new IdempotentRedisDAO(stringRedisTemplate);
    }

    // ========== 各种 IdempotentKeyResolver Bean ==========

    @Bean
    public DefaultIdempotentKeyResolver defaultIdempotentKeyResolver() {
        return new DefaultIdempotentKeyResolver();
    }

    @Bean
    public UserIdempotentKeyResolver userIdempotentKeyResolver() {
        return new UserIdempotentKeyResolver();
    }

    @Bean
    public ExpressionIdempotentKeyResolver expressionIdempotentKeyResolver() {
        return new ExpressionIdempotentKeyResolver();
    }

}
