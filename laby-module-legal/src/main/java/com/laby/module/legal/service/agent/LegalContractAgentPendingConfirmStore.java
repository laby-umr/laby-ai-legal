package com.laby.module.legal.service.agent;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.service.agent.bo.LegalContractAgentPendingConfirmBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Confirm 快照存储：Redis 优先，进程内 Map 兜底（同实例热路径）。
 */
@Slf4j
@Component
public class LegalContractAgentPendingConfirmStore {

    private static final String REDIS_KEY_PREFIX = "laby:agentscope:legal:pending-confirm:";
    private static final Duration TTL = Duration.ofHours(2);

    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final ConcurrentHashMap<String, String> localCache = new ConcurrentHashMap<>();

    public LegalContractAgentPendingConfirmStore(ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    public void save(LegalContractAgentPendingConfirmBO snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getSessionId())) {
            return;
        }
        String json = JsonUtils.toJsonString(snapshot);
        localCache.put(snapshot.getSessionId(), json);
        StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
        if (redis != null) {
            redis.opsForValue().set(redisKey(snapshot.getSessionId()), json, TTL);
        }
    }

    public Optional<LegalContractAgentPendingConfirmBO> find(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return Optional.empty();
        }
        StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
        if (redis != null) {
            String fromRedis = redis.opsForValue().get(redisKey(sessionId));
            if (StrUtil.isNotBlank(fromRedis)) {
                return parse(fromRedis);
            }
        }
        String fromLocal = localCache.get(sessionId);
        if (StrUtil.isNotBlank(fromLocal)) {
            return parse(fromLocal);
        }
        return Optional.empty();
    }

    public void remove(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }
        localCache.remove(sessionId);
        StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
        if (redis != null) {
            redis.delete(redisKey(sessionId));
        }
    }

    private static Optional<LegalContractAgentPendingConfirmBO> parse(String json) {
        try {
            return Optional.ofNullable(JsonUtils.parseObject(json, LegalContractAgentPendingConfirmBO.class));
        } catch (Exception ex) {
            log.warn("[parse] Confirm 快照解析失败: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String redisKey(String sessionId) {
        return REDIS_KEY_PREFIX + sessionId;
    }

}
