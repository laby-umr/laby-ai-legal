# `laby-module-ai` AgentScope 原位重构 — Phase 0～1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在功能分支内于 `laby-module-ai` 引入 AgentScope 2 运行时契约（`AiLlmClient`），并完成 `laby-module-legal` 法务 AI 链路（审核 Pipeline + 合同 Agent + 普通 Chat）对 Spring AI 的替换。

**Architecture:** 新建 `com.laby.module.ai.core` 作为对外稳定 LLM 契约；`framework.agentscope` 实现 Model 解析与 `AgentScopeLlmClient`；法务 Agent 在 `laby-module-legal` 内使用 `HarnessAgent` + Middleware，Tool 仍留在 legal 包。Phase 0 保留旧 `getChatModel()` 供尚未迁移的 `AiChatMessageServiceImpl` 等使用；Phase 1 完成后 legal 模块零 Spring AI import。

**Tech Stack:** AgentScope Java 2.0.0-RC1（`agentscope-harness`）、Spring Boot 3.5、Reactor Flux、现有 `ai_model` / `ai_api_key` 表、Redis Session（Phase 1）

**依据 Spec:** [`docs/superpowers/specs/2026-06-05-laby-module-ai-agentscope-inplace-refactor-spec.md`](../specs/2026-06-05-laby-module-ai-agentscope-inplace-refactor-spec.md)

---

## 文件结构（Phase 0～1 新增/修改）

| 文件 | 职责 |
|------|------|
| `laby-dependencies/pom.xml` | 新增 `agentscope.version` BOM |
| `laby-module-ai/pom.xml` | 引入 `agentscope-harness`（Phase 0 暂**不删** spring-ai） |
| `laby-module-legal/pom.xml` | 引入 `agentscope-harness` |
| `core/llm/AiLlmClient.java` | 对外 LLM 契约 |
| `core/llm/AiLlmRequest.java` | 请求 DTO |
| `core/llm/AiMessage.java` | 消息 DTO（role + content + reasoning） |
| `core/llm/AiLlmStreamEvent.java` | 流式事件（content / reasoning / done） |
| `core/llm/AiMessageRoleEnum.java` | SYSTEM / USER / ASSISTANT / TOOL |
| `framework/agentscope/model/AgentScopeModelFactory.java` | `AiModelDO` → AgentScope model 实例 |
| `framework/agentscope/model/AgentScopeLlmClient.java` | `AiLlmClient` 实现 |
| `framework/agentscope/config/AgentScopeProperties.java` | `laby.ai.agentscope.*` 配置 |
| `service/model/AiModelService.java` | 新增 `getLlmClient(Long)` |
| `service/model/AiModelServiceImpl.java` | 实现 `getLlmClient`，委托 Registry |
| `legal/.../LegalAiAuditPipelineService.java` | 改用 `AiLlmClient` |
| `legal/.../LegalContractAgentServiceImpl.java` | 改用 `HarnessAgent` |
| `legal/.../LegalContractChatServiceImpl.java` | 普通模式改用 `AiLlmClient.stream` |
| `legal/framework/agentscope/*Middleware*.java` | Tenant / Trace / SSE |

---

## Phase 0 — Core 契约 + ModelRegistry（预计 3～5 天）

### Task 1: BOM 与 Maven 依赖

**Files:**
- Modify: `laby-dependencies/pom.xml`
- Modify: `laby-module-ai/pom.xml`
- Modify: `laby-module-legal/pom.xml`

- [ ] **Step 1: 在 `laby-dependencies/pom.xml` 的 `<properties>` 增加**

```xml
<agentscope.version>2.0.0-RC1</agentscope.version>
```

- [ ] **Step 2: 在 `<dependencyManagement>` 增加**

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
    <version>${agentscope.version}</version>
</dependency>
```

- [ ] **Step 3: `laby-module-ai/pom.xml` 的 `<properties>` 增加并添加依赖**

```xml
<properties>
    ...
    <agentscope.version>2.0.0-RC1</agentscope.version>
</properties>

<!-- dependencies 内，TinyFlow 之前 -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
</dependency>
```

- [ ] **Step 4: `laby-module-legal/pom.xml` 同样添加 `agentscope-harness`（无 version，走 BOM）**

- [ ] **Step 5: 验证依赖可解析**

Run:

```bash
cd d:/IdeaProject/laby-admin
mvn -pl laby-module-ai,laby-module-legal dependency:resolve -q
```

Expected: BUILD SUCCESS，输出含 `io.agentscope:agentscope-harness:jar:2.0.0-RC1`

- [ ] **Step 6: Commit**

```bash
git add laby-dependencies/pom.xml laby-module-ai/pom.xml laby-module-legal/pom.xml
git commit -m "build: add AgentScope 2.0.0-RC1 BOM and module dependencies"
```

---

### Task 2: Core LLM 契约 DTO

**Files:**
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/core/llm/AiMessageRoleEnum.java`
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/core/llm/AiMessage.java`
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/core/llm/AiLlmStreamEvent.java`
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/core/llm/AiLlmRequest.java`
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/core/llm/AiLlmClient.java`

- [ ] **Step 1: 创建 `AiMessageRoleEnum.java`**

```java
package com.laby.module.ai.core.llm;

public enum AiMessageRoleEnum {
    SYSTEM, USER, ASSISTANT, TOOL
}
```

- [ ] **Step 2: 创建 `AiMessage.java`**

```java
package com.laby.module.ai.core.llm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AiMessage {
    private AiMessageRoleEnum role;
    private String content;
    /** tool 名称，role=TOOL 时使用 */
    private String toolName;
}
```

- [ ] **Step 3: 创建 `AiLlmStreamEvent.java`**

```java
package com.laby.module.ai.core.llm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AiLlmStreamEvent {
    public enum Type { CONTENT, REASONING, DONE, ERROR }

    private Type type;
    private String delta;
    private String errorMessage;
}
```

- [ ] **Step 4: 创建 `AiLlmRequest.java`**

```java
package com.laby.module.ai.core.llm;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AiLlmRequest {
    private List<AiMessage> messages = new ArrayList<>();
    private Double temperature;
    private Integer maxTokens;
    /** 审核 Pipeline 要求 JSON 数组输出 */
    private boolean jsonMode;
    private Map<String, Object> metadata = new HashMap<>();
}
```

- [ ] **Step 5: 创建 `AiLlmClient.java`**

```java
package com.laby.module.ai.core.llm;

import reactor.core.publisher.Flux;

public interface AiLlmClient {
    String call(AiLlmRequest request);
    Flux<AiLlmStreamEvent> stream(AiLlmRequest request);
}
```

- [ ] **Step 6: 编译**

Run: `mvn -pl laby-module-ai compile -q`
Expected: SUCCESS

- [ ] **Step 7: Commit**

```bash
git add laby-module-ai/src/main/java/com/laby/module/ai/core/
git commit -m "feat(ai): add core AiLlmClient contract and DTOs"
```

---

### Task 3: AgentScopeModelFactory（P0 平台）

**Files:**
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/framework/agentscope/model/AgentScopeModelFactory.java`
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/framework/agentscope/model/AgentScopeModelConfig.java`

- [ ] **Step 1: 创建 `AgentScopeModelConfig.java`（不可变配置）**

```java
package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.enums.model.AiPlatformEnum;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentScopeModelConfig {
    private AiPlatformEnum platform;
    private String modelName;
    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;
}
```

- [ ] **Step 2: 创建 `AgentScopeModelFactory.java`**

```java
package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.dashscope.DashScopeChatModel;
import io.agentscope.core.model.openai.OpenAIChatModel;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.ai.enums.ErrorCodeConstants.MODEL_NOT_EXISTS;

public final class AgentScopeModelFactory {

    private AgentScopeModelFactory() {}

    public static AgentScopeModelConfig from(AiModelDO model, AiApiKeyDO apiKey) {
        return AgentScopeModelConfig.builder()
                .platform(AiPlatformEnum.validatePlatform(apiKey.getPlatform()))
                .modelName(model.getModel())
                .apiKey(apiKey.getApiKey())
                .baseUrl(apiKey.getUrl())
                .temperature(model.getTemperature())
                .maxTokens(model.getMaxTokens())
                .build();
    }

    /**
     * 构造 AgentScope Model 实例（P0：通义 / OpenAI 兼容 / DeepSeek / 智谱 / Ollama）
     */
    public static Model buildChatModel(AgentScopeModelConfig config) {
        AiPlatformEnum platform = config.getPlatform();
        return switch (platform) {
            case TONG_YI -> DashScopeChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .defaultTemperature(config.getTemperature())
                    .defaultMaxTokens(config.getMaxTokens())
                    .build();
            case OPENAI, DEEP_SEEK, ZHI_PU, DOU_BAO, SILICON_FLOW, HUN_YUAN, MOONSHOT ->
                    OpenAIChatModel.builder()
                            .apiKey(config.getApiKey())
                            .baseUrl(resolveOpenAiCompatibleBaseUrl(platform, config.getBaseUrl()))
                            .modelName(config.getModelName())
                            .defaultTemperature(config.getTemperature())
                            .defaultMaxTokens(config.getMaxTokens())
                            .build();
            case OLLAMA -> OpenAIChatModel.builder()
                    .apiKey("ollama")
                    .baseUrl(normalizeOllamaBaseUrl(config.getBaseUrl()))
                    .modelName(config.getModelName())
                    .build();
            default -> throw exception(MODEL_NOT_EXISTS); // Phase 1 前 P1 平台仍走旧 Spring AI
        };
    }

    /** 供 HarnessAgent.builder().model("dashscope:qwen-plus") 使用的字符串形式 */
    public static String toModelRef(AgentScopeModelConfig config) {
        return switch (config.getPlatform()) {
            case TONG_YI -> "dashscope:" + config.getModelName();
            case OPENAI -> "openai:" + config.getModelName();
            case DEEP_SEEK -> "openai:" + config.getModelName(); // DeepSeek OpenAI-compatible
            case OLLAMA -> "ollama:" + config.getModelName();
            default -> null;
        };
    }

    private static String resolveOpenAiCompatibleBaseUrl(AiPlatformEnum platform, String url) {
        if (url != null && !url.isBlank()) {
            return url;
        }
        return switch (platform) {
            case DEEP_SEEK -> "https://api.deepseek.com";
            case ZHI_PU -> "https://open.bigmodel.cn/api/paas/v4";
            default -> "https://api.openai.com/v1";
        };
    }

    private static String normalizeOllamaBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:11434/v1";
        }
        return url.endsWith("/v1") ? url : url.replaceAll("/+$", "") + "/v1";
    }
}
```

Note: 若 RC1 中 `DashScopeChatModel` / `OpenAIChatModel` 包名与上不同，以 IDE 自动 import 为准调整；原则不变。

- [ ] **Step 3: 编译并根据 IDE 修正 import**

Run: `mvn -pl laby-module-ai compile`
Expected: SUCCESS（若 FAIL，按 AgentScope Javadoc 修正 builder 方法名）

- [ ] **Step 4: Commit**

```bash
git add laby-module-ai/src/main/java/com/laby/module/ai/framework/agentscope/model/
git commit -m "feat(ai): add AgentScopeModelFactory for P0 platforms"
```

---

### Task 4: AgentScopeLlmClient 实现

**Files:**
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/framework/agentscope/model/AgentScopeLlmClient.java`
- Create: `laby-module-ai/src/main/java/com/laby/module/ai/framework/agentscope/model/AiMessageConverter.java`

- [ ] **Step 1: 创建 `AiMessageConverter.java`**

```java
package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.SystemMessage;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.message.Msg;

import java.util.ArrayList;
import java.util.List;

public final class AiMessageConverter {

    private AiMessageConverter() {}

    public static List<Msg> toAgentScopeMessages(List<AiMessage> messages) {
        List<Msg> result = new ArrayList<>(messages.size());
        for (AiMessage message : messages) {
            result.add(switch (message.getRole()) {
                case SYSTEM -> new SystemMessage(message.getContent());
                case USER -> new UserMessage(message.getContent());
                case ASSISTANT -> new AssistantMessage(message.getContent());
                case TOOL -> new UserMessage("[tool:" + message.getToolName() + "] " + message.getContent());
            });
        }
        return result;
    }
}
```

- [ ] **Step 2: 创建 `AgentScopeLlmClient.java`**

```java
package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.core.llm.*;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AgentScopeLlmClient implements AiLlmClient {

    private final AgentScopeModelConfig config;
    private final Path workspaceRoot;

    @Override
    public String call(AiLlmRequest request) {
        HarnessAgent agent = buildAgent(request, false);
        List<Msg> messages = AiMessageConverter.toAgentScopeMessages(request.getMessages());
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(UUID.randomUUID().toString())
                .build();
        Msg last = messages.remove(messages.size() - 1);
        // 简化：system 进 sysPrompt，其余合并为单轮 user（Phase 1 前足够审核 Pipeline）
        String sys = extractSystem(request.getMessages());
        if (sys != null) {
            agent = HarnessAgent.builder()
                    .name("laby-llm-batch")
                    .sysPrompt(sys)
                    .model(AgentScopeModelFactory.buildChatModel(config))
                    .workspace(workspaceRoot)
                    .build();
        }
        return agent.call(last, ctx).block().getTextContent();
    }

    @Override
    public Flux<AiLlmStreamEvent> stream(AiLlmRequest request) {
        HarnessAgent agent = buildAgent(request, true);
        Msg userMsg = toSingleUserMessage(request.getMessages());
        return agent.streamEvents(userMsg)
                .map(event -> {
                    if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                        return new AiLlmStreamEvent()
                                .setType(AiLlmStreamEvent.Type.CONTENT)
                                .setDelta(((TextBlockDeltaEvent) event).getDelta());
                    }
                    return null;
                })
                .filter(e -> e != null)
                .concatWith(Flux.just(new AiLlmStreamEvent().setType(AiLlmStreamEvent.Type.DONE)));
    }

    private HarnessAgent buildAgent(AiLlmRequest request, boolean streaming) {
        Model model = AgentScopeModelFactory.buildChatModel(config);
        String sysPrompt = extractSystem(request.getMessages());
        return HarnessAgent.builder()
                .name(streaming ? "laby-llm-stream" : "laby-llm-call")
                .sysPrompt(sysPrompt != null ? sysPrompt : "You are a helpful assistant.")
                .model(model)
                .workspace(workspaceRoot)
                .build();
    }

    private static String extractSystem(List<AiMessage> messages) {
        return messages.stream()
                .filter(m -> m.getRole() == AiMessageRoleEnum.SYSTEM)
                .map(AiMessage::getContent)
                .findFirst()
                .orElse(null);
    }

    private static Msg toSingleUserMessage(List<AiMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (AiMessage m : messages) {
            if (m.getRole() == AiMessageRoleEnum.USER) {
                sb.append(m.getContent()).append('\n');
            }
        }
        return new io.agentscope.core.message.UserMessage(sb.toString().trim());
    }
}
```

- [ ] **Step 3: 创建 `AgentScopeProperties.java`**

```java
package com.laby.module.ai.framework.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "laby.ai.agentscope")
public class AgentScopeProperties {
    /** Harness 工作区根目录 */
    private String workspacePath = "${java.io.tmpdir}/laby-agentscope";
    private String sessionKeyPrefix = "as:";
}
```

- [ ] **Step 4: 注册 ConfigurationProperties（在 `AiAutoConfiguration` 或新建 `AgentScopeAutoConfiguration`）**

Create: `laby-module-ai/src/main/java/com/laby/module/ai/framework/agentscope/config/AgentScopeAutoConfiguration.java`

```java
package com.laby.module.ai.framework.agentscope.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentScopeProperties.class)
public class AgentScopeAutoConfiguration {
}
```

- [ ] **Step 5: 编译**

Run: `mvn -pl laby-module-ai compile`
Expected: SUCCESS（按实际 AgentScope API 微调 `call` / `getTextContent` 方法）

- [ ] **Step 6: Commit**

```bash
git add laby-module-ai/src/main/java/com/laby/module/ai/framework/agentscope/
git commit -m "feat(ai): implement AgentScopeLlmClient"
```

---

### Task 5: AiModelService 暴露 getLlmClient

**Files:**
- Modify: `laby-module-ai/src/main/java/com/laby/module/ai/service/model/AiModelService.java`
- Modify: `laby-module-ai/src/main/java/com/laby/module/ai/service/model/AiModelServiceImpl.java`
- Modify: `laby-server/src/main/resources/application.yaml`（或 `application-local.yaml`）

- [ ] **Step 1: `AiModelService.java` 接口末尾新增**

```java
import com.laby.module.ai.core.llm.AiLlmClient;

/**
 * 获取 AgentScope 驱动的 LLM 客户端（新运行时）
 */
AiLlmClient getLlmClient(Long modelId);
```

- [ ] **Step 2: `AiModelServiceImpl.java` 注入并实现**

```java
@Resource
private AgentScopeProperties agentScopeProperties;

@Override
public AiLlmClient getLlmClient(Long id) {
    AiModelDO model = validateModel(id);
    AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
    AgentScopeModelConfig config = AgentScopeModelFactory.from(model, apiKey);
    Path workspace = Paths.get(agentScopeProperties.getWorkspacePath(), "model-" + id);
    return new AgentScopeLlmClient(config, workspace);
}
```

- [ ] **Step 3: `application-local.yaml` 增加**

```yaml
laby:
  ai:
    agentscope:
      workspace-path: ${java.io.tmpdir}/laby-agentscope
```

- [ ] **Step 4: 编译全链路**

Run: `mvn -pl laby-module-ai,laby-module-legal,laby-server compile -q`
Expected: SUCCESS

- [ ] **Step 5: Commit**

```bash
git add laby-module-ai/src/main/java/com/laby/module/ai/service/model/ laby-server/src/main/resources/
git commit -m "feat(ai): expose getLlmClient on AiModelService"
```

---

### Task 6: Phase 0 集成测试

**Files:**
- Create: `laby-module-ai/src/test/java/com/laby/module/ai/framework/agentscope/model/AgentScopeLlmClientIT.java`

- [ ] **Step 1: 编写集成测试（需本地 API Key，默认 `@Disabled`）**

```java
package com.laby.module.ai.framework.agentscope.model;

import com.laby.module.ai.core.llm.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("需要 DASHSCOPE_API_KEY 或 OPENAI_API_KEY")
class AgentScopeLlmClientIT {

    @Test
    void call_shouldReturnText() {
        AgentScopeModelConfig config = AgentScopeModelConfig.builder()
                .platform(com.laby.module.ai.enums.model.AiPlatformEnum.TONG_YI)
                .modelName("qwen-plus")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .build();
        AgentScopeLlmClient client = new AgentScopeLlmClient(config, Paths.get(System.getProperty("java.io.tmpdir")));
        AiLlmRequest request = new AiLlmRequest()
                .setMessages(List.of(
                        new AiMessage().setRole(AiMessageRoleEnum.USER).setContent("回复 OK 两个字母")));
        String text = client.call(request);
        assertNotNull(text);
        assertFalse(text.isBlank());
    }
}
```

- [ ] **Step 2: 运行单元测试（不含 IT）**

Run: `mvn -pl laby-module-ai test -Dtest='!*IT' -q`
Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add laby-module-ai/src/test/java/com/laby/module/ai/framework/agentscope/
git commit -m "test(ai): add AgentScopeLlmClient integration test skeleton"
```

**Phase 0 完成标准:** `getLlmClient(id).call(...)` 在本地带 Key 的 IT 中可通；工程 compile 通过；Spring AI 依赖仍在但 legal 尚未切换。

---

## Phase 1 — 法务链路迁移（预计 5～7 天）

### Task 7: 审核 Pipeline 改用 AiLlmClient

**Files:**
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/orchestrator/bo/LegalAiAuditPipelineCommand.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/orchestrator/LegalAiAuditPipelineService.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalAiAuditServiceImpl.java`

- [ ] **Step 1: `LegalAiAuditPipelineCommand` — 删除 Spring AI 字段，改为**

```java
import com.laby.module.ai.core.llm.AiLlmClient;

private AiLlmClient llmClient;
// 删除: ChatModel chatModel; ChatOptions chatOptions;
```

- [ ] **Step 2: `LegalAiAuditServiceImpl` 构建 command 时**

```java
// 原: command.setChatModel(aiModelService.getChatModel(...))
command.setLlmClient(aiModelService.getLlmClient(model.getId()));
```

- [ ] **Step 3: `LegalAiAuditPipelineService.callAuditBatchWithRetry` 改写核心调用**

将原：

```java
ChatResponse response = chatModel.call(new Prompt(List.of(
    new SystemMessage(batchSystemPrompt),
    new UserMessage(userPrompt))));
String content = AiUtils.getChatResponseContent(response);
```

改为：

```java
AiLlmRequest request = new AiLlmRequest()
    .setJsonMode(true)
    .setMessages(List.of(
        new AiMessage().setRole(AiMessageRoleEnum.SYSTEM).setContent(batchSystemPrompt),
        new AiMessage().setRole(AiMessageRoleEnum.USER).setContent(userPrompt)));
String content = command.getLlmClient().call(request);
```

- [ ] **Step 4: reasoning 流式（若原用 StreamingChatModel）**

在 `stream` 循环中监听 `AiLlmStreamEvent.Type.REASONING`（若 AgentScope 事件映射已扩展）写入 `LegalAiAuditProgressHolder`；MVP 可仅在 batch 开始/结束写进度。

- [ ] **Step 5: 编译 legal**

Run: `mvn -pl laby-module-legal compile -q`
Expected: SUCCESS

- [ ] **Step 6: 跑 Playbook 评测**

Run: `mvn -pl laby-module-legal test -Dtest=LegalPlaybookEvalRunnerTest -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add laby-module-legal/src/main/java/com/laby/module/legal/service/orchestrator/ laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalAiAuditServiceImpl.java
git commit -m "feat(legal): migrate audit pipeline to AiLlmClient"
```

---

### Task 8: 合同 Chat 普通模式

**Files:**
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalContractChatServiceImpl.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalContractChatHistoryHelper.java`

- [ ] **Step 1: 在普通模式分支（非 Agent）将 `getChatModel().stream(prompt)` 替换为**

```java
AiLlmClient llmClient = aiModelService.getLlmClient(model.getId());
AiLlmRequest request = buildAiLlmRequest(history, systemPrompt, userQuestion);
Flux<CommonResult<LegalContractChatRespVO>> llmContent = llmClient.stream(request)
    .map(event -> {
        if (event.getType() == AiLlmStreamEvent.Type.CONTENT) {
            return success(new LegalContractChatRespVO()
                .setContent(event.getDelta())
                .setSessionId(sessionId));
        }
        return null;
    })
    .filter(Objects::nonNull);
```

- [ ] **Step 2: `LegalContractChatHistoryHelper` — 新增 `toAiMessages(...)` 替代 `toSpringAiMessages(...)`，删除 Spring AI import**

- [ ] **Step 3: 编译 + 手工：合同页普通问答 SSE 有 content 流**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(legal): migrate contract chat normal mode to AiLlmClient"
```

---

### Task 9: 法务 Agent → HarnessAgent

**Files:**
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/framework/agentscope/LegalAgentScopeConfig.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/framework/agentscope/middleware/LegalTenantMiddleware.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/framework/agentscope/middleware/LegalAgentTraceMiddleware.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/framework/agentscope/middleware/LegalAgentSseMiddleware.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/agent/LegalContractAgentServiceImpl.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/Legal*Tool.java`（8 个）
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/agent/LegalAgentToolProvider.java`

- [ ] **Step 1: `LegalAgentScopeConfig` — 从 contract + model 构建 HarnessAgent**

```java
public HarnessAgent buildAgent(LegalContractDO contract, AiModelDO model,
                               AiApiKeyDO apiKey, List<Tool> tools,
                               String sysPrompt, String sessionId) {
    AgentScopeModelConfig config = AgentScopeModelFactory.from(model, apiKey);
    return HarnessAgent.builder()
            .name("legal-contract-agent")
            .sysPrompt(sysPrompt)
            .model(AgentScopeModelFactory.buildChatModel(config))
            .tools(tools)
            .middleware(List.of(
                    new LegalTenantMiddleware(),
                    new LegalAgentTraceMiddleware(agentStepLogService),
                    new LegalAgentSseMiddleware(sessionId)))
            .workspace(Paths.get(workspacePath, "legal", String.valueOf(contract.getId())))
            .build();
}
```

- [ ] **Step 2: 8 个 Tool — 从 `@Tool` + `ToolCallback` 改为 AgentScope Tool 接口**

参考 AgentScope 文档 `Tool` / `@ToolParam`；每个 Tool 保留原有 Service 注入逻辑，删除 `org.springframework.ai.tool.annotation.Tool`（若存在）。

- [ ] **Step 3: `LegalAgentToolProvider` — 返回 `List<Tool>` 而非 `List<ToolCallback>`**

- [ ] **Step 4: `LegalContractAgentServiceImpl.runStream` 核心替换**

```java
HarnessAgent agent = legalAgentScopeConfig.buildAgent(...);
RuntimeContext ctx = RuntimeContext.builder()
        .sessionId(sessionId)
        .userId(String.valueOf(SecurityFrameworkUtils.getLoginUserId()))
        .build();
UserMessage userMessage = new UserMessage(reqVO.getQuestion());
Flux<CommonResult<LegalContractChatRespVO>> llmContent = agent.streamEvents(userMessage, ctx)
    .map(event -> mapAgentEventToSse(event, sessionId))
    ...
```

- [ ] **Step 5: `LegalAgentSseMiddleware` — 在 tool 执行前后写 `LegalAgentSseEventHolder`（替代 `LegalAgentToolAspect` 的 AOP）**

- [ ] **Step 6: 提案 Tool — 接 AgentScope Permission / `LegalAgentProposalService`**

在 `LegalProposeAdoptOpinionTool` / `LegalProposeSkipParagraphTool` 执行前触发 permission 事件，前端仍收 `proposal` SSE。

- [ ] **Step 7: 删除 `LegalAgentToolAspect.java` 中 Spring AI 相关逻辑（或整类删除若 Middleware 覆盖）**

- [ ] **Step 8: E2E 手工验收**

| 步骤 | 预期 |
|------|------|
| 合同页 Agent 模式提问 | SSE `content` 流 |
| 触发 `legal_search_paragraphs` | SSE `tool_start` / `tool_end` |
| 触发提案 Tool | SSE `proposal`，Confirm 后生效 |
| `legal_agent_step_log` 表 | 有 step 记录 |

- [ ] **Step 9: Commit**

```bash
git commit -m "feat(legal): migrate contract agent to AgentScope HarnessAgent"
```

---

### Task 10: 清理 legal 模块 Spring AI import

**Files:**
- Modify: 所有 `laby-module-legal` 中含 `org.springframework.ai` 的文件（约 22 个）

- [ ] **Step 1: 运行门禁脚本**

```bash
rg "org\.springframework\.ai" laby-module-legal/src/main
```

Expected: **无输出**

- [ ] **Step 2: 若 `LegalAgentToolSupport` / `AiUtils` 仍引用 Spring AI 工具上下文，改为**

```java
// 使用 AiUtils.TOOL_CONTEXT_TENANT_ID 常量（AiUtils 内删除 Spring 部分后保留常量）
Map<String, Object> ctx = Map.of(
    AiUtils.TOOL_CONTEXT_TENANT_ID, TenantContextHolder.getTenantId(),
    AiUtils.TOOL_CONTEXT_LOGIN_USER, SecurityFrameworkUtils.getLoginUser());
```

- [ ] **Step 3: 全量编译**

Run: `mvn -pl laby-module-ai,laby-module-legal,laby-server compile -q`
Expected: SUCCESS

- [ ] **Step 4: Commit**

```bash
git commit -m "chore(legal): remove all Spring AI imports from legal module"
```

**Phase 1 完成标准:** legal 零 Spring AI；审核 + Agent + 普通 Chat 手工 E2E 通过；`laby-module-ai` 内 Spring AI **仍可存在**（Phase 3 再删）。

---

## Phase 2～5 预览（后续单独计划）

| Phase | 内容 | 计划文件 |
|-------|------|----------|
| 2 | Embedding + Qdrant RAG + 知识库 | `2026-06-XX-phase2-rag-plan.md` |
| 3 | `AiChatMessageServiceImpl` 等通用 Chat | 同上系列 |
| 4 | 图片 + MCP + 删 spring-ai pom | 同上系列 |
| 5 | 删 `AiModelFactoryImpl` + import 门禁全绿 | 同上系列 |

---

## Spec 覆盖自检

| Spec 要求 | 本计划 Task |
|-----------|-------------|
| G3 稳定 core API | Task 2, 5 |
| G5 legal 零 Spring AI | Task 7～10 |
| P0 平台 ModelFactory | Task 3 |
| 法务 Agent HarnessAgent | Task 9 |
| 审核 BatchCompletion | Task 7 |
| SSE 契约不变 | Task 8, 9 Middleware |
| Phase 0 getLlmClient 验收 | Task 6 |
| 删 spring-ai pom（终态 G1） | Phase 4 计划（不在本文件） |

---

## Git 分支建议

```
feature/agentscope-ai-refactor
  ├── commit: Phase 0 Tasks 1～6
  └── commit: Phase 1 Tasks 7～10
```

每 Task 或每 2 Task 一个 PR，便于 review。
