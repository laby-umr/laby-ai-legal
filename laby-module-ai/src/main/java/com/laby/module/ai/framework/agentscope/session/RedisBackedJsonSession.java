package com.laby.module.ai.framework.agentscope.session;

import cn.hutool.core.util.StrUtil;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.State;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * JsonSession 本地缓存 + Redis 持久化（RC1 无原生 RedisSession 时的适配）。
 */
@Slf4j
public class RedisBackedJsonSession implements Session {

    private static final String INDEX_SUFFIX = ":index";

    private final JsonSession delegate;
    private final StringRedisTemplate redisTemplate;
    private final String redisDataPrefix;
    private final Duration ttl;
    private final Set<String> dirtyRelativePaths = ConcurrentHashMap.newKeySet();
    private volatile boolean fullFlushRequired;

    public RedisBackedJsonSession(Path localSessionDir, StringRedisTemplate redisTemplate,
                                  String redisDataPrefix, Duration ttl) {
        this.delegate = new JsonSession(localSessionDir);
        this.redisTemplate = redisTemplate;
        this.redisDataPrefix = redisDataPrefix;
        this.ttl = ttl;
        restoreFromRedis();
    }

    @Override
    public void save(SessionKey sessionKey, String stateKey, State state) {
        delegate.save(sessionKey, stateKey, state);
        markDirty(sessionKey, stateKey);
        flushToRedis();
    }

    @Override
    public void save(SessionKey sessionKey, String stateKey, List<? extends State> states) {
        delegate.save(sessionKey, stateKey, states);
        markDirty(sessionKey, stateKey);
        flushToRedis();
    }

    @Override
    public <T extends State> Optional<T> get(SessionKey sessionKey, String stateKey, Class<T> type) {
        return delegate.get(sessionKey, stateKey, type);
    }

    @Override
    public <T extends State> List<T> getList(SessionKey sessionKey, String stateKey, Class<T> type) {
        return delegate.getList(sessionKey, stateKey, type);
    }

    @Override
    public boolean exists(SessionKey sessionKey) {
        return delegate.exists(sessionKey);
    }

    @Override
    public void delete(SessionKey sessionKey) {
        delegate.delete(sessionKey);
        fullFlushRequired = true;
        flushToRedis();
    }

    @Override
    public void delete(SessionKey sessionKey, String stateKey) {
        delegate.delete(sessionKey, stateKey);
        markDirty(sessionKey, stateKey);
        flushToRedis();
    }

    @Override
    public Set<SessionKey> listSessionKeys() {
        return delegate.listSessionKeys();
    }

    @Override
    public void close() {
        fullFlushRequired = true;
        flushToRedis();
        delegate.close();
    }

    private void markDirty(SessionKey sessionKey, String stateKey) {
        String relative = resolveRelativePath(sessionKey, stateKey);
        if (relative != null) {
            dirtyRelativePaths.add(relative);
        } else {
            fullFlushRequired = true;
        }
    }

    private String resolveRelativePath(SessionKey sessionKey, String stateKey) {
        if (sessionKey == null || StrUtil.isBlank(stateKey)) {
            return null;
        }
        Path sessionRoot = delegate.getSessionDirectory();
        Path candidate = sessionRoot.resolve(sessionKey.toIdentifier()).resolve(stateKey + ".json");
        if (Files.isRegularFile(candidate)) {
            return sessionRoot.relativize(candidate).toString().replace('\\', '/');
        }
        return null;
    }

    private void restoreFromRedis() {
        String indexKey = redisDataPrefix + INDEX_SUFFIX;
        Set<String> members = redisTemplate.opsForSet().members(indexKey);
        if (members == null || members.isEmpty()) {
            return;
        }
        Path sessionRoot = delegate.getSessionDirectory();
        for (String member : members) {
            int split = member.indexOf('|');
            if (split <= 0 || split >= member.length() - 1) {
                continue;
            }
            String relativePath = member.substring(split + 1);
            String redisKey = redisDataPrefix + member.substring(0, split) + ":" + relativePath;
            String content = redisTemplate.opsForValue().get(redisKey);
            if (content == null) {
                continue;
            }
            try {
                Path target = sessionRoot.resolve(relativePath);
                Files.createDirectories(target.getParent());
                Files.writeString(target, content, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                log.warn("[restoreFromRedis] 恢复 {} 失败: {}", relativePath, ex.getMessage());
            }
        }
    }

    private void flushToRedis() {
        Path sessionRoot = delegate.getSessionDirectory();
        if (!Files.isDirectory(sessionRoot)) {
            return;
        }
        if (fullFlushRequired) {
            flushAllToRedis(sessionRoot);
            return;
        }
        if (dirtyRelativePaths.isEmpty()) {
            return;
        }
        Set<String> pending = Set.copyOf(dirtyRelativePaths);
        dirtyRelativePaths.removeAll(pending);
        String indexKey = redisDataPrefix + INDEX_SUFFIX;
        for (String relative : pending) {
            Path path = sessionRoot.resolve(relative);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            syncFileToRedis(sessionRoot, relative, indexKey);
        }
    }

    private void flushAllToRedis(Path sessionRoot) {
        String indexKey = redisDataPrefix + INDEX_SUFFIX;
        Set<String> newIndex = new HashSet<>();
        try (Stream<Path> paths = Files.walk(sessionRoot)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String relative = sessionRoot.relativize(path).toString().replace('\\', '/');
                if (syncFileToRedis(sessionRoot, relative, indexKey)) {
                    newIndex.add("data|" + relative);
                }
            });
        } catch (IOException ex) {
            log.warn("[flushAllToRedis] 遍历 session 目录失败: {}", ex.getMessage());
            return;
        }
        redisTemplate.delete(indexKey);
        if (!newIndex.isEmpty()) {
            redisTemplate.opsForSet().add(indexKey, newIndex.toArray(String[]::new));
            redisTemplate.expire(indexKey, ttl);
        }
        dirtyRelativePaths.clear();
        fullFlushRequired = false;
    }

    private boolean syncFileToRedis(Path sessionRoot, String relative, String indexKey) {
        Path path = sessionRoot.resolve(relative);
        String redisKey = redisDataPrefix + "data:" + relative;
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            redisTemplate.opsForValue().set(redisKey, content, ttl);
            redisTemplate.opsForSet().add(indexKey, "data|" + relative);
            redisTemplate.expire(indexKey, ttl);
            return true;
        } catch (IOException ex) {
            log.warn("[syncFileToRedis] 同步 {} 失败: {}", relative, ex.getMessage());
            return false;
        }
    }

}
