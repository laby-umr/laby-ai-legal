# Spec：`laby-module-ai` 原位重构 — AgentScope 2 Java 完全替换 Spring AI

| 属性 | 值 |
|------|-----|
| 版本 | v1.0 |
| 日期 | 2026-06-05 |
| 状态 | **已评审 — Phase 0～1 计划已出** |
| 分支策略 | 功能分支内直接改 `laby-module-ai`（不新建 Maven 模块、不停服关模块） |
| 终态目标 | `laby-module-ai` 与 `laby-module-legal` **零** `org.springframework.ai` 依赖；`pom.xml` 移除全部 `spring-ai-*` / `spring-ai-alibaba-*` / `springaicommunity-*` |
| 运行时 | **AgentScope Java 2.0**（`io.agentscope:agentscope-harness:2.0.0-RC1`，GA 后可升） |

>  supersede：[`2026-06-05-agentscope-java-ai-refactor-spec.md`](./2026-06-05-agentscope-java-ai-refactor-spec.md) 中「新建独立模块 / 双运行时长期并存」方案；**以本文为准**。

---

## 1. 背景与目标

### 1.1 为什么要改

当前 `laby-module-ai` 以 **Spring AI 1.1.5** 为 LLM 运行时，存在：

- `AiModelFactoryImpl` 单文件 ~866 行，耦合 15+ 平台与 VectorStore / Embedding
- `AiUtils` 直接依赖各平台 `*ChatOptions`（Spring AI / DashScope / springaicommunity）
- 法务模块（`laby-module-legal`）通过 `AiModelService.getChatModel()` 等 **泄漏 Spring AI 类型**到业务层（约 22 个 Java 文件含 `org.springframework.ai`）

团队决策：**在同一 Maven 模块内重构**，用 AgentScope 2 Java 作为唯一 LLM/Agent 运行时，**彻底删除 Spring AI**。

### 1.2 目标（Must）

| # | 目标 |
|---|------|
| G1 | `laby-module-ai/pom.xml` 无 Spring AI 相关 dependency |
| G2 | `laby-module-ai/src/main` 无 `import org.springframework.ai.*` |
| G3 | 对外稳定 API：`AiModelService` 及新增 `com.laby.module.ai.core.*` 接口，**不暴露** AgentScope / Spring AI 类型 |
| G4 | 保留现有 REST 路径、DB 表（`ai_*`）、管理后台 CRUD 行为 |
| G5 | `laby-module-legal` 同步改造，消除对 Spring AI 的直接 import |
| G6 | 功能分支合并前：`mvn -pl laby-module-ai,laby-module-legal,laby-server compile` 通过 |

### 1.3 非目标（Won't）

| # | 说明 |
|---|------|
| N1 | 不改 OnlyOffice / 合同文档 E10 |
| N2 | 不改 Playbook 确定性引擎（无 LLM） |
| N3 | 不重做前端页面结构（SSE eventType 保持兼容） |
| N4 | TinyFlow 工作流引擎本身不重写（仅替换其 LLM Provider 桥接，可放在最后一 Phase） |
| N5 | 不在本 Spec 内要求 Day-1 支持全部 20 个 `AiPlatformEnum`（分 P0/P1/P2 平台梯队，见 §6） |

---

## 2. 现状盘点

### 2.1 模块规模

| 指标 | 数量 |
|------|------|
| `laby-module-ai` Java 文件 | ~190 main + ~32 test |
| main 中引用 `org.springframework.ai` | **31 个文件** |
| `laby-module-legal` 引用 Spring AI | **22 个文件** |
| `pom.xml` Spring AI 系依赖 | **14+**（含 alibaba dashscope、qianfan/moonshot community starter） |

### 2.2 Spring AI 能力 → 业务映射

| Spring AI 能力 | 使用方 | 关键类 |
|----------------|--------|--------|
| `ChatModel` / `StreamingChatModel` | 对话、写作、导图、法务 Agent/审核/Chat | `AiChatMessageServiceImpl`, `LegalContractAgentServiceImpl`, `LegalAiAuditPipelineService` |
| `ToolCallback` | 通用 Chat Tool、法务 8 Tool | `AiChatMessageServiceImpl`, `Legal*Tool` |
| `EmbeddingModel` | 知识库、法务段落向量 | `AiKnowledgeSegmentServiceImpl`, `LegalContractParagraphEmbeddingServiceImpl` |
| `VectorStore` | 知识库 RAG（Qdrant/Redis/Milvus） | `AiKnowledgeSegmentServiceImpl` |
| `ImageModel` | AI 绘画 | `AiImageServiceImpl` |
| `TikaDocumentReader` | 知识库文档解析 | `AiKnowledgeDocumentServiceImpl` |
| `TokenTextSplitter` | 文档分片 | `AiKnowledgeSegmentServiceImpl` |
| MCP Server/Client | 通用 Chat MCP | `AiChatMessageServiceImpl`, `AiAutoConfiguration` |
| 各平台 `*ChatOptions` | 全模块 | `AiUtils`, `AiModelFactoryImpl` |

### 2.3 保留不动（非 Spring AI）

| 组件 | 说明 |
|------|------|
| `MidjourneyApi` / `SunoApi` | 自研 HTTP 客户端，继续保留 |
| `AiWebSearchClient` | 博查等联网搜索，保留 |
| PPT API（讯飞/文多多） | 保留 |
| TinyFlow + agents-flex | 非 Spring AI；Provider 桥接后续替换 |
| `ai_model` / `ai_api_key` / `ai_knowledge` 等表 |  schema 不变 |

### 2.4 AgentScope 2 约束（来自 RC1 文档）

- **RAG 模块**（`Knowledge` / `KnowledgeRetrievalTools`）在 2.0-RC **已 deprecated**，v2 重写中 → 本项目的 RAG **用 Qdrant Java Client + 自研 Embedding 层**，不依赖 AgentScope RAG API。
- **HarnessAgent** 为推荐生产入口（Session、Middleware、Permission、Workspace）。
- 流式 API：`streamEvents()` 替代 Spring AI `stream(Prompt)`。

---

## 3. 目标架构（模块内包结构）

### 3.1 分层图

```
Controller（admin/chat/knowledge/...）  ← 不变
        │
Service（AiChatMessageServiceImpl 等）  ← 改调用 core API
        │
┌───────▼──────────────────────────────────────────┐
│  com.laby.module.ai.core（新建，对外稳定契约）       │
│  - AiLlmClient / AiLlmRequest / AiLlmStreamEvent │
│  - AiEmbeddingClient                             │
│  - AiVectorStoreClient                           │
│  - AiImageClient（可选，P2）                      │
│  - AiToolDefinition / AiToolExecutor             │
└───────┬──────────────────────────────────────────┘
        │
┌───────▼──────────────────────────────────────────┐
│  com.laby.module.ai.framework.agentscope（新建）   │
│  - AgentScopeModelRegistry（读 ai_model 表）      │
│  - AgentScopeLlmClient                           │
│  - AgentScopeAgentRunner（ReAct + Middleware）    │
│  - AgentScopeSessionStore（Redis）               │
│  - QdrantVectorStoreClient（直连 qdrant-client）  │
│  - EmbeddingClient 各平台适配                      │
└──────────────────────────────────────────────────┘

删除：framework/ai/core/model/AiModelFactoryImpl 及 Spring AI 包装 ChatModel
删除：framework/ai/config/AiAutoConfiguration 中 Spring AI Bean
删除：util/AiUtils 中 Spring AI Options 构建
```

### 3.2 `AiModelService` 接口变更（Breaking，legal 必须同步）

**删除方法：**

```java
ChatModel getChatModel(Long id);
ImageModel getImageModel(Long id);
VectorStore getOrCreateVectorStore(Long id, Map<String, Class<?>> metadataFields);
```

**新增方法：**

```java
AiLlmClient getLlmClient(Long modelId);
AiEmbeddingClient getEmbeddingClient(Long modelId);
AiVectorStoreClient getVectorStoreClient(Long knowledgeId);  // 内部仍读 ai_knowledge.model_id
AiImageClient getImageClient(Long modelId);                  // Phase 4
```

CRUD 方法（`createModel` / `getRequiredDefaultModel` / `validateModel` 等）**签名不变**。

### 3.3 `AiLlmClient` 契约（示意）

```java
public interface AiLlmClient {
    /** 单次 completion（审核 Pipeline、写作） */
    String call(AiLlmRequest request);

    /** 流式：文本 delta + reasoning（若模型支持） */
    Flux<AiLlmStreamEvent> stream(AiLlmRequest request);
}

public class AiLlmRequest {
    private List<AiMessage> messages;      // system/user/assistant/tool
    private Double temperature;
    private Integer maxTokens;
    private List<AiToolDefinition> tools; // 可选
    private Map<String, Object> toolContext; // tenantId, userId, contractId...
    private boolean jsonMode;              // 审核 JSON 数组
}
```

法务与通用 Chat **只依赖** `com.laby.module.ai.core`，不 import AgentScope。

### 3.4 Agent 场景（法务 8 Tool）

法务 Agent 有两种实现路径（**Spec 选定 A**）：

| 方案 | 说明 | 结论 |
|------|------|------|
| A | `laby-module-legal` 内构建 AgentScope `HarnessAgent`，Tool 仍注册在 legal；`AiLlmClient` 仅用于非 Agent 场景 | ✅ **推荐**：Agent 域逻辑留在 legal |
| B | `laby-module-ai` 提供通用 `AiAgentRunner`，legal 传入 Tool 列表 | 耦合高，暂不采用 |

`laby-module-legal` 可 **直接依赖** `agentscope-harness`（仅 Agent 包），或通过 `laby-module-ai` 暴露 `AiAgentRunner` 门面——**默认：legal 直接依赖 agentscope-harness**（Agent 是法务域能力），AI 模块提供 ModelRegistry 工具类 `AgentScopeModelFactory`（static/helper，读 `AiModelDO`）。

---

## 4. Spring AI → 替代方案对照

| 原 Spring AI | 新实现 | 所在包 |
|--------------|--------|--------|
| `ChatModel` / `StreamingChatModel` | `AgentScopeLlmClient` + OpenAI-compatible Model | `framework.agentscope.model` |
| `Prompt` / `Message` | `AiLlmRequest` / `AiMessage` | `core.llm` |
| `ToolCallback` | AgentScope Tool + `AiToolDefinition` 适配 | `core.tool` / legal Tool |
| `EmbeddingModel` | `AiEmbeddingClient`（DashScope/OpenAI/… HTTP） | `framework.agentscope.embedding` |
| `VectorStore` | `QdrantVectorStoreClient`（`io.qdrant:client`） | `framework.agentscope.rag` |
| `TikaDocumentReader` | `org.apache.tika:tika-core` 直连 | `service.knowledge` |
| `TokenTextSplitter` | 自研 `TokenTextSplitter` 或 jtokkit | `service.knowledge.splitter` |
| `ImageModel` | `AiImageClient`（OpenAI Images / 通义 / Stability HTTP） | `framework.agentscope.image` |
| MCP | Phase 4：AgentScope MCP 或保留最小自研 SSE 客户端 | TBD in Phase 4 |
| `AiUtils.buildChatOptions` | 删除；参数进 `AiLlmRequest` + ModelRegistry | — |
| `AiModelFactoryImpl` | `AgentScopeModelRegistry` | `framework.agentscope.model` |

---

## 5. 依赖变更（`laby-module-ai/pom.xml`）

### 5.1 移除（终态）

```xml
<!-- 全部删除 -->
spring-ai-starter-model-*
spring-ai-alibaba-starter-dashscope
spring-ai-starter-vector-store-*
spring-ai-tika-document-reader
spring-ai-starter-mcp-server-webmvc
spring-ai-starter-mcp-client
qianfan-spring-boot-starter
moonshot-spring-boot-starter
```

### 5.2 新增

```xml
<properties>
    <agentscope.version>2.0.0-RC1</agentscope.version>
</properties>

<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
    <version>${agentscope.version}</version>
</dependency>

<!-- RAG：不用 Spring AI VectorStore -->
<dependency>
    <groupId>io.qdrant</groupId>
    <artifactId>client</artifactId>
    <version><!-- 在 laby-dependencies BOM 中统一管理，与现网 Qdrant Server 版本对齐 --></version>
</dependency>

<!-- 文档解析：替代 spring-ai-tika -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
</dependency>
```

### 5.3 `laby-module-legal/pom.xml`

```xml
<!-- 法务 Agent 需要 -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
    <version>${agentscope.version}</version>
</dependency>
```

版本号统一由父 POM / `laby-dependencies` BOM 管理。

---

## 6. 平台支持梯队

Day-1 **必须**（P0，与现网法务/默认模型对齐）：

| 平台 | 接入方式 |
|------|----------|
| 通义千问 `TongYi` | DashScope OpenAI-compatible / AgentScope 插件 |
| DeepSeek | OpenAI-compatible endpoint |
| OpenAI | 官方 API |
| 智谱 `ZhiPu` | OpenAI-compatible |
| Ollama | OpenAI-compatible local |

Phase 内逐步补齐（P1）：豆包、混元、硅基、MiniMax、Moonshot、百川、星火、文心

P2（可延后上线菜单提示「平台迁移中」）：Azure、Anthropic、Gemini、Grok、StableDiffusion 图片

**非 LLM**（保持现有 HTTP 客户端）：Midjourney、Suno

---

## 7. 分 Phase 实施（同一功能分支）

### Phase 0 — Core 契约 + ModelRegistry（3～5 天）

**交付：**

- [ ] 新建 `com.laby.module.ai.core.*` 接口与 DTO
- [ ] 新建 `framework.agentscope.model.AgentScopeModelRegistry`
- [ ] `AgentScopeLlmClient` 实现 P0 平台
- [ ] 改造 `AiModelService` / `AiModelServiceImpl`（删除 Spring AI 方法，新增 `getLlmClient`）
- [ ] 删除 `AiModelFactoryImpl`、`AiModelFactory` 骨架替换
- [ ] 单元测试：`AgentScopeLlmClientTest`（mock 或集成）

**验收：** 独立调用 `getLlmClient(defaultChatModelId).call(...)` 成功。

### Phase 1 — 法务链路（5～7 天）

**交付：**

- [ ] `LegalAiAuditPipelineService` → `AiLlmClient.stream/call` + JSON 解析
- [ ] `LegalContractAgentServiceImpl` → AgentScope HarnessAgent + 8 Tool
- [ ] 删除 legal 中全部 `org.springframework.ai` import
- [ ] Middleware：Tenant、Trace（`legal_agent_step_log`）、SSE bridge
- [ ] `LegalContractChatServiceImpl` 普通模式 → `AiLlmClient.stream`

**验收：** 合同 Agent E2E + 首轮 AI 审核 + SSE tool 事件。

### Phase 2 — Embedding + 知识库 RAG（5～7 天）

**交付：**

- [ ] `AiEmbeddingClient` + `QdrantVectorStoreClient`
- [ ] 重写 `AiKnowledgeDocumentServiceImpl`（Tika 直连）
- [ ] 重写 `AiKnowledgeSegmentServiceImpl`（分片 + 向量写入/检索）
- [ ] `LegalContractParagraphEmbeddingServiceImpl` → `getEmbeddingClient`
- [ ] `LegalAuditContextServiceImpl` 知识检索 → `AiVectorStoreClient`

**验收：** 知识库上传文档 → 分段 → 检索命中；法务段落向量检索正常。

### Phase 3 — 通用 AI Chat + Tool（5～7 天）

**交付：**

- [ ] 重写 `AiChatMessageServiceImpl`（流式 + 内置 Tool）
- [ ] 重写 `AiChatRoleServiceImpl` 润色等
- [ ] 重写 `AiWriteServiceImpl`、`AiMindMapServiceImpl`
- [ ] 删除 `tool/function/*` 的 Spring AI Tool 注解，改 AgentScope Tool 或 `AiToolExecutor`

**验收：** 后台 AI 对话、写作、导图功能回归。

### Phase 4 — 图片 + MCP + 剩余平台（5～10 天）

**交付：**

- [ ] `AiImageServiceImpl` → `AiImageClient`
- [ ] MCP Server/Client 迁移或临时下线（见 §8 决策 D3）
- [ ] P1 平台补齐
- [ ] 删除 `AiAutoConfiguration` 剩余 Spring AI 配置
- [ ] 删除/重写 spring-ai 相关 **test** 类

**验收：** 绘画流程可用；`grep org.springframework.ai laby-module-ai` 为空。

### Phase 5 — 清理 + 全量验收（2～3 天）

**交付：**

- [ ] 删除废弃包 `framework/ai/core/model/*ChatModel`（Spring 包装类）
- [ ] 删除 `AiUtils` 中 Spring AI 代码；保留 `TOOL_CONTEXT_*` 常量
- [ ] TinyFlow：`getLLmProvider4Tinyflow` 改用 OpenAI-compatible HTTP 或 AgentScope Provider
- [ ] 更新模块 `description` 与 README
- [ ] `mvn dependency:tree -pl laby-module-ai | grep springframework.ai` 无输出

---

## 8. 待确认决策

| ID | 问题 | 建议默认 |
|----|------|----------|
| D1 | AgentScope 版本 | `2.0.0-RC1`，GA 后升 minor |
| D2 | Session 存储 | Redis，key 前缀 `as:session:` |
| D3 | MCP | Phase 4 评估；若 AgentScope MCP 未就绪，**临时下线 MCP 菜单**并在 release note 说明 |
| D4 | Milvus / Redis VectorStore | 现网若仅用 Qdrant，**只实现 Qdrant**；其他枚举配置项报错提示 |
| D5 | 多平台一次全做 vs 梯队 | **梯队**（§6），P2 平台可返回 `AI_PLATFORM_NOT_SUPPORTED_YET` |
| D6 | 法务 Agent 依赖 agentscope-harness | **是**（legal pom 直接引入） |

---

## 9. 删除清单（Phase 5 前完成）

| 路径 | 动作 |
|------|------|
| `framework/ai/core/model/AiModelFactoryImpl.java` | 删除 |
| `framework/ai/core/model/AiModelFactory.java` | 替换为 `AgentScopeModelRegistry` |
| `framework/ai/core/model/doubao/DouBaoChatModel.java` 等 Spring 包装 | 删除 |
| `framework/ai/core/model/siliconflow/SiliconFlow*Model.java` | 删除（API 类可保留） |
| `framework/ai/config/AiAutoConfiguration.java` | 删除 Spring AI Bean，保留 Gemini/豆包等 **非 Spring AI** 条件配置或合并 |
| `util/AiUtils.java` | 重写：仅保留 message 转换、context 常量 |
| `src/test/.../model/chat/*ChatModelTests.java` | 删除或改为 `AiLlmClient` 集成测试 |
| `src/test/.../model/mcp/DouBaoMcpTests.java` | Phase 4 重写或删除 |

**保留并迁移：**

| 路径 | 说明 |
|------|------|
| `framework/ai/core/model/midjourney/**` | HTTP API |
| `framework/ai/core/model/suno/**` | HTTP API |
| `framework/ai/core/webserch/**` | 联网搜索 |
| `controller/**`、`dal/**` | 管理 CRUD |

---

## 10. 配置项

```yaml
laby:
  ai:
    agentscope:
      version: 2.0.0-RC1
      session-store: redis
      session-key-prefix: "as:"
      default-max-steps: 12
    vector-store:
      type: qdrant          # 仅实现 qdrant；milvus/redis 暂不支持
      qdrant:
        host: ${QDRANT_HOST:127.0.0.1}
        port: ${QDRANT_PORT:6334}
```

现有 `ai_model` / `ai_api_key` 表与后台配置 UI **不改**。

---

## 11. 前端与 API 兼容性

| 项 | 要求 |
|----|------|
| REST 路径 | `/admin-api/ai/**`、`/admin-api/legal/**` 不变 |
| SSE eventType | `content` / `tool_start` / `tool_end` / `proposal` 不变 |
| 错误码 | 新增 `AI_PLATFORM_NOT_SUPPORTED_YET` 等，不破坏现有码段 |

---

## 12. 测试策略

| 层级 | 内容 |
|------|------|
| 单元 | `AgentScopeModelRegistryTest`、`AiLlmClientTest`、`QdrantVectorStoreClientTest` |
| 模块 | 保留/改写 `LegalPlaybookEvalRunnerTest`（legal 模块） |
| 手工 | 合同 Agent 黄金路径、AI 审核、知识库 RAG、通用 Chat |
| 门禁 | CI：`mvn -pl laby-module-ai,laby-module-legal test` + Spring AI import 检查脚本 |

**Import 门禁脚本（合并前必跑）：**

```bash
rg "org\.springframework\.ai" laby-module-ai/src/main laby-module-legal/src/main && exit 1 || exit 0
```

---

## 13. 风险

| 风险 | 缓解 |
|------|------|
| AgentScope 2.0 仍 RC | 锁版本；关键路径集成测试；关注 GA |
| AgentScope RAG deprecated | 不用其 RAG；Qdrant 直连 |
| 多平台回归面大 | 平台梯队 + 特性开关 |
| `AiModelService` Breaking | Phase 0 一次性改接口，Phase 1 前 legal 不编译通过则 CI 红，强制同步 |
| MCP 迁移不确定 | D3：可临时下线 |

---

## 14. 验收标准（合并 main 前）

- [ ] `rg org.springframework.ai laby-module-ai/src/main laby-module-legal/src/main` 无匹配
- [ ] `laby-module-ai/pom.xml` 无 `spring-ai` / `springaicommunity` artifact
- [ ] P0 平台 + 法务 Agent + 审核 + 知识库 RAG + 通用 Chat 手工通过
- [ ] SSE 与提案 Confirm 流程正常
- [ ] 租户隔离无串租（Middleware + ToolContext 回归）

---

## 15. 相关文档

| 文档 | 关系 |
|------|------|
| [AgentScope Java 2 Quickstart](https://java.agentscope.io/v2/zh/docs/quickstart.html) | 依赖与 Harness 用法 |
| `2026-06-03-legal-contract-agent-spec.md` | 法务 Tool 列表、SSE 协议仍有效 |
| `2026-06-05-agentscope-java-ai-refactor-spec.md` | 已被本文 supersede（新模块方案） |

---

## 16. 评审通过后下一步

1. 确认 §8 决策 D1～D6  
2. 使用 **writing-plans** 生成 Phase 0～1 任务级 Implementation Plan  
3. 在功能分支按 Phase 提交 PR（建议每 Phase 一个 PR）
