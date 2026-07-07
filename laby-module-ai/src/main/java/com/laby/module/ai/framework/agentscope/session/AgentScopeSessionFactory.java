package com.laby.module.ai.framework.agentscope.session;

import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import io.agentscope.core.session.InMemorySession;
import io.agentscope.core.session.Session;
import io.agentscope.harness.agent.session.WorkspaceSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;

/**
 * 为 HarnessAgent 提供 Session 持久化（WorkspaceSession / InMemorySession / RedisBackedJsonSession）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentScopeSessionFactory {

    private final AgentScopeProperties properties;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;

    public Session createSession(Path workspaceRoot) {
        String store = properties.getSessionStore();
        if ("memory".equalsIgnoreCase(store)) {
            return new InMemorySession();
        }
        if ("redis".equalsIgnoreCase(store)) {
            StringRedisTemplate redisTemplate = stringRedisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                log.warn("[createSession] session-store=redis 但 StringRedisTemplate 不可用，降级 WorkspaceSession");
                return createWorkspaceSession(workspaceRoot);
            }
            Path root = workspaceRoot != null ? workspaceRoot : Path.of(properties.getWorkspacePath());
            Path localCache = root.resolve("session-cache");
            String workspaceToken = Integer.toHexString(root.toAbsolutePath().normalize().hashCode());
            String redisPrefix = properties.getSessionKeyPrefix() + "json:" + workspaceToken + ":";
            Duration ttl = Duration.ofHours(Math.max(properties.getSessionTtlHours(), 1));
            return new RedisBackedJsonSession(localCache, redisTemplate, redisPrefix, ttl);
        }
        return createWorkspaceSession(workspaceRoot);
    }

    private Session createWorkspaceSession(Path workspaceRoot) {
        Path root = workspaceRoot != null ? workspaceRoot : Path.of(properties.getWorkspacePath());
        try {
            return new WorkspaceSession(root, "sessions");
        } catch (Exception ex) {
            log.warn("[createSession] WorkspaceSession 初始化失败，降级 InMemorySession: {}", ex.getMessage());
            return new InMemorySession();
        }
    }

}
