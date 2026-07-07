package com.laby.module.ai.framework.agentscope.mcp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.tools.McpServerConfig;
import io.agentscope.harness.agent.tools.McpServerRegistrar;
import io.agentscope.harness.agent.tools.ToolsConfig;
import io.agentscope.harness.agent.workspace.WorkspaceConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将角色配置的 MCP Client 注册到 AgentScope {@link Toolkit}。
 * <p>
 * 配置来源优先级：{@code spring.ai.mcp.client} &gt; {@code laby.ai.mcp} &gt; workspace {@code tools.json}。
 */
@Slf4j
@Component
public class AgentScopeMcpToolRegistrar {

    private static final String SPRING_SSE_CONNECTIONS_PREFIX = "spring.ai.mcp.client.sse.connections.";
    private static final String LABY_MCP_SERVERS_PREFIX = "laby.ai.mcp.servers.";

    @Resource
    private Environment environment;

    @Resource
    private AgentScopeProperties agentScopeProperties;

    /**
     * 连接 MCP 服务并将远程工具注册到 {@code toolkit}。单个 client 失败仅 warn，不阻断其余注册。
     */
    public void registerMcpTools(Toolkit toolkit, List<String> mcpClientNames, Path workspace) {
        if (toolkit == null || CollUtil.isEmpty(mcpClientNames)) {
            return;
        }
        Map<String, McpServerConfig> selected = new LinkedHashMap<>();
        for (String rawName : mcpClientNames) {
            String clientName = StrUtil.trim(rawName);
            if (StrUtil.isBlank(clientName)) {
                continue;
            }
            try {
                McpServerConfig config = resolveClientConfig(clientName, workspace);
                if (config == null) {
                    log.warn("[registerMcpTools] MCP client '{}' 未在配置中找到，跳过", clientName);
                    continue;
                }
                selected.put(clientName, config);
            } catch (Exception ex) {
                log.warn("[registerMcpTools] 解析 MCP client '{}' 配置失败: {}", clientName, ex.getMessage());
            }
        }
        if (selected.isEmpty()) {
            return;
        }
        McpServerRegistrar.register(toolkit, selected);
    }

    private McpServerConfig resolveClientConfig(String clientName, Path workspace) {
        McpServerConfig config = resolveFromSpringAiMcp(clientName);
        if (config != null) {
            return config;
        }
        config = resolveFromLabyAiMcp(clientName);
        if (config != null) {
            return config;
        }
        return resolveFromWorkspaceToolsJson(clientName, workspace);
    }

    private McpServerConfig resolveFromSpringAiMcp(String clientName) {
        String base = SPRING_SSE_CONNECTIONS_PREFIX + clientName;
        String url = environment.getProperty(base + ".url");
        if (StrUtil.isBlank(url)) {
            return null;
        }
        McpServerConfig config = new McpServerConfig();
        config.setTransport("sse");
        String endpoint = StrUtil.blankToDefault(environment.getProperty(base + ".sse-endpoint"), "/sse");
        config.setUrl(joinUrl(url, endpoint));
        return config;
    }

    private McpServerConfig resolveFromLabyAiMcp(String clientName) {
        String base = LABY_MCP_SERVERS_PREFIX + clientName;
        String transport = environment.getProperty(base + ".transport");
        if (StrUtil.isBlank(transport)) {
            return null;
        }
        McpServerConfig config = new McpServerConfig();
        config.setTransport(transport);
        config.setUrl(environment.getProperty(base + ".url"));
        config.setCommand(environment.getProperty(base + ".command"));
        config.setArgs(readStringListProperty(base + ".args"));
        config.setEnv(readStringMapProperty(base + ".env"));
        config.setHeaders(readStringMapProperty(base + ".headers"));
        config.setQueryParams(readStringMapProperty(base + ".query-params"));
        config.setEnableTools(readStringListProperty(base + ".enable-tools"));
        config.setTimeout(readDurationProperty(base + ".timeout"));
        config.setInitializationTimeout(readDurationProperty(base + ".initialization-timeout"));
        return config;
    }

    private McpServerConfig resolveFromWorkspaceToolsJson(String clientName, Path workspace) {
        for (Path toolsJson : workspaceToolsJsonCandidates(workspace)) {
            if (!Files.isRegularFile(toolsJson)) {
                continue;
            }
            try {
                String content = Files.readString(toolsJson, StandardCharsets.UTF_8);
                if (StrUtil.isBlank(content)) {
                    continue;
                }
                ToolsConfig toolsConfig = JsonUtils.parseObject(content, ToolsConfig.class);
                if (toolsConfig == null || toolsConfig.getMcpServers() == null) {
                    continue;
                }
                McpServerConfig config = toolsConfig.getMcpServers().get(clientName);
                if (config != null) {
                    return config;
                }
            } catch (Exception ex) {
                log.warn("[registerMcpTools] 读取 {} 失败: {}", toolsJson, ex.getMessage());
            }
        }
        return null;
    }

    private List<Path> workspaceToolsJsonCandidates(Path workspace) {
        if (workspace != null) {
            return List.of(workspace.resolve(WorkspaceConstants.TOOLS_JSON));
        }
        String workspaceRoot = agentScopeProperties.getWorkspacePath();
        if (StrUtil.isBlank(workspaceRoot)) {
            return Collections.emptyList();
        }
        return List.of(Path.of(workspaceRoot).resolve(WorkspaceConstants.TOOLS_JSON));
    }

    private static String joinUrl(String baseUrl, String endpoint) {
        String normalizedBase = StrUtil.removeSuffix(baseUrl.trim(), "/");
        String normalizedEndpoint = StrUtil.blankToDefault(endpoint, "/sse");
        if (!normalizedEndpoint.startsWith("/")) {
            normalizedEndpoint = "/" + normalizedEndpoint;
        }
        return normalizedBase + normalizedEndpoint;
    }

    private List<String> readStringListProperty(String key) {
        String value = environment.getProperty(key);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return StrUtil.splitTrim(value, ',');
    }

    private Map<String, String> readStringMapProperty(String key) {
        String value = environment.getProperty(key);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return JsonUtils.parseObject(value, new TypeReference<Map<String, String>>() {});
    }

    private Duration readDurationProperty(String key) {
        String value = environment.getProperty(key);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return Duration.parse(value);
    }

}
