package com.laby.module.legal.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laby.module.legal.service.orchestration.InMemoryLegalOrchestrationToolContextStore;
import com.laby.module.legal.service.orchestration.LegalOrchestrationToolContextHolder;
import com.laby.module.legal.service.orchestration.LegalOrchestrationToolContextStore;
import com.laby.module.legal.service.orchestration.RedisLegalOrchestrationToolContextStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class LegalOrchestrationToolContextConfiguration {

    @Resource
    private LegalOrchestrationProperties orchestrationProperties;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public LegalOrchestrationToolContextStore legalOrchestrationToolContextStore() {
        if (stringRedisTemplate != null) {
            return new RedisLegalOrchestrationToolContextStore(
                    stringRedisTemplate, objectMapper, orchestrationProperties);
        }
        return new InMemoryLegalOrchestrationToolContextStore();
    }

    @Resource
    private LegalOrchestrationToolContextStore legalOrchestrationToolContextStore;

    @PostConstruct
    void wireHolder() {
        LegalOrchestrationToolContextHolder.setStore(legalOrchestrationToolContextStore);
    }

}
