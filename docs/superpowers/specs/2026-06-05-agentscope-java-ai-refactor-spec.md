# AI 全栈重构方案：AgentScope 2 Java 替换 Spring AI

| 属性 | 值 |
|------|-----|
| 版本 | v0.2 Draft |
| 日期 | 2026-06-05 |
| 状态 | 待评审（已被 [`2026-06-05-laby-module-ai-agentscope-inplace-refactor-spec.md`](./2026-06-05-laby-module-ai-agentscope-inplace-refactor-spec.md) **supersede** — 采用模块内原位重构） |
| **终态目标** | **`pom.xml` 零 `org.springframework.ai` 依赖**（完全替换，不留 Spring AI 孤岛） |
| 范围 | `laby-module-ai` + `laby-module-legal` AI 子系统 + 相关前端 SSE 协议 |
| 目标运行时 | **AgentScope Java 2.x**（`agentscope-harness`，2.0-RC 起） |
| 替换对象 | **Spring AI 1.1.5 全部移除**；agents-flex（TinyFlow）非 Spring AI，可暂留 |

---

## 1. 背景与判断

### 1.1 现状结论

当前 AI 能力分两层：

| 层级 | 技术 | 职责 |
|------|------|------|
| **通用 AI 平台** | `laby-module-ai` + Spring AI | 多模型工厂、对话、RAG、MCP、写作/导图/图片等 |
| **法务 AI 域** | `laby-module-legal` | 合同 Agent（8 Tool）、批量审核 Pipeline、Playbook、Trace、SkillPack、BPM 二轮 |

Spring AI 在项目中主要提供：**ChatModel / StreamingChatModel、ToolCallback、VectorStore、MCP 客户端**。法务 Agent 与审核 Pipeline 均通过 `AiModelService` 间接调用，**未直接散落 Spring AI 类型**（约 20 个 legal 文件、33 个 ai 模块文件有 `org.springframework.ai` 引用）。

### 1.2 为何选 AgentScope 2 Java

团队判断（与 Spring AI 对比）：

- **本质相同**：均为 LLM + Tool 循环 + 流式输出；差异在封装形态。
- **AgentScope 优势**：Builder 一体化、Middleware、细粒度 `AgentEvent`、Session 持久化、HITL/权限、Workspace/沙箱、子 Agent —— **生产 Agent 平台能力更全**。
- **Spring AI 优势**：已与 Spring Security / 租户 / MyBatis / 现有 `ai_*` 表深度集成；**业务单体改造成本低**。

重构目标不是「换 API 名词」，而是：

1. **完全移除 Spring AI**（终态：`grep org.springframework.ai` 仅 test 归档或为零）
2. **统一 Agent 运行时**（法务 + 通用对话全部走 AgentScope 2 Java）
3. **保留业务与数据模型**（`ai_model`、`legal_agent_proposal`、BPM、Trace 表不动）
4. **分阶段交付，每阶段删旧代码**（不是长期双栈；回滚窗口仅该 Phase 验收期）

### 1.3 完全替换 vs「关模块」

| 做法 | 结论 |
|------|------|
| 关掉 `laby-module-ai` 或 legal AI | ❌ 业务中断，且删不掉 Spring AI 依赖 |
| 长期保留 Spring AI 孤岛（图片/音乐） | ❌ 与「完全替换」目标冲突 |
| **分 Phase 迁移 → 每 Phase 删除对应 Spring AI 代码 → 最终删 pom** | ✅ 推荐 |

**过渡期的 `spring-ai-runtime` 适配器**：仅用于 Phase 1～4 验收期回滚（建议每 Phase ≤2 周），**Phase 5 必须删除**，不作为终态架构。

### 1.4 非目标

- 不重写 Playbook **确定性引擎**（无 LLM，保留 Java 规则）
- 不改造 OnlyOffice / 文档 E10
- TinyFlow + agents-flex：**不是 Spring AI**，可 Phase 6 单独换 AgentScope Provider（非阻塞 Spring AI 下线）

---

## 2. 目标架构（Target）

### 2.1 分层

```
┌─────────────────────────────────────────────────────────────┐
│  laby-ui（SSE / REST 协议保持不变或版本化 v2）                  │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  laby-module-legal / laby-module-ai  Controller（薄）         │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  laby-ai-runtime-api（新，终态唯一运行时入口）                  │
│  - AiRuntimeFacade.streamChat / runAgent / runBatchCompletion │
│  - AiRuntimeContext（tenantId, userId, contractId, scene）     │
│  - AiEventPublisher → 映射现有 SSE eventType                   │
└───────────────┬───────────────────────────────────────────────┘
                │
    ┌───────────▼──────────┐
    │ agentscope-runtime   │  ← 终态唯一实现
    │ AgentScope 2 Java    │
    └───────────┬──────────┘
    （Phase 1～4 验收期可临时保留 spring-ai-runtime，Phase 5 删除）
                │
    ┌───────────▼──────────────────────────────────────────────┐
    │ ModelAdapterRegistry ← 读 ai_model / ai_api_key           │
    │ SessionStore ← Redis/MySQL（AgentScope Session）          │
    │ ToolRegistry ← 法务 Tool + 通用 Tool                        │
    │ RetrievalAdapter ← Qdrant（段落 + 知识库）                  │
    └──────────────────────────────────────────────────────────┘
```

### 2.2 新模块建议

| 模块 | 说明 |
|------|------|
| `laby-module-ai-runtime-api` | 运行时抽象接口、Event DTO、Context，**无法律/无 Spring AI 依赖** |
| `laby-module-ai-runtime-agentscope` | AgentScope 2 实现：Agent Builder、Middleware、Tool 注册、Model 适配 |
| `laby-module-ai-runtime-springai` | **临时**：Phase 1～4 回滚用，Phase 5 删除整个模块 |
| `laby-module-ai`（重写） | 保留 CRUD + 业务 Service；**删除** `AiModelFactoryImpl`、`AiAutoConfiguration`、全部 Spring AI starter |
| `laby-module-legal` | 业务编排不变；Agent/Audit/Chat **只依赖 runtime-api** |

### 2.3 AgentScope 核心映射

| 现有（Spring AI） | 目标（AgentScope 2 Java） |
|-------------------|---------------------------|
| `StreamingChatModel.stream(prompt)` | `Agent.streamEvents()` / `reply_stream` 对齐事件 |
| `ToolCallback` + `BiFunction` | AgentScope Tool / ToolGroup |
| `ToolContext`（tenant/contract） | `RuntimeContext` + Middleware 注入 |
| `LegalAgentToolAspect`（AOP SSE） | `onActing` / `onReasoning` Middleware |
| `LegalAgentProposalService`（HITL） | AgentScope Permission + 事件流暂停/恢复 |
| `LegalAgentStepLogService` | Middleware 写 `legal_agent_step_log` |
| `ChatModel.call()` 批量审核 | 专用 **BatchCompletionRunner**（非 ReAct，单次 JSON） |
| `AiModelFactoryImpl` | `AgentScopeModelAdapter` 按 platform 构建 Model |
| `VectorStore` + Embedding | `QdrantClient` + AgentScope Retrieval / 自研 `EmbeddingService`（**不用** Spring AI VectorStore） |
| `ImageModel` | AgentScope 多模态或各平台 HTTP SDK（Stability/通义等） |
| `TikaDocumentReader` | Apache Tika 直连（去掉 spring-ai-tika 包装） |
| `SyncMcpToolCallbackProvider` | AgentScope MCP 或自研 MCP 客户端 |

---

## 3. 范围清单（Inventory）

### 3.1 必须迁移（P0～P1）

| 域 | 类/能力 | 说明 |
|----|---------|------|
| 法务 Agent | `LegalContractAgentServiceImpl` | ReAct + 8 Tool + SSE |
| 法务 Tool | `tool/agent/Legal*Tool` × 8 | 改为 AgentScope Tool，保留 Service 调用 |
| 法务 Chat | `LegalContractChatServiceImpl` | 普通模式 + Agent 模式路由 |
| 审核 Pipeline | `LegalAiAuditPipelineService` | 分批 LLM + JSON 解析 + reasoning 进度 |
| 模型网关 | `AiModelService` / `AiModelFactoryImpl` | 背后换 AgentScope Model |
| Trace | `LegalAiTraceRecorder` | Middleware 挂钩 |
| SkillPack | `LegalSkillPackRegistry` | 映射 ToolGroup / Model 策略 |

### 3.2 第二阶段迁移（P2）

| 域 | 类/能力 |
|----|---------|
| 通用对话 | `AiChatMessageServiceImpl`（RAG + MCP + 多 Tool） |
| 知识库 RAG | `AiKnowledgeSegmentServiceImpl` |
| 段落向量 | `LegalContractParagraphEmbeddingServiceImpl` |
| 写作/导图 | `AiWriteServiceImpl`, `AiMindMapServiceImpl` |

### 3.3 Phase 5 必须一并替换（否则无法删 pom）

| 域 | Spring AI 用法 | AgentScope / 替代 |
|----|----------------|-------------------|
| 图片 | `ImageModel` × 多平台 | HTTP SDK 或 AgentScope 图像 API |
| 写作/导图 | `ChatModel.stream` | `CompletionRunner` |
| 知识库 RAG | `VectorStore` + `EmbeddingModel` | Qdrant Java Client + Embedding 适配 |
| 文档解析 | `TikaDocumentReader` | Apache Tika |
| 分词 | `TokenTextSplitter` | 自研或 jtokkit |
| MCP | `spring-ai-starter-mcp-*` | AgentScope MCP / 自研 |
| 15+ Chat 平台 | `AiModelFactoryImpl` | `AgentScopeModelAdapter` 按 platform 分支 |

### 3.4 非 Spring AI（Phase 6 可选）

| 域 | 说明 |
|----|------|
| TinyFlow + agents-flex | 当前走 `QwenLlm`/`OllamaLlm`，**不阻塞** Spring AI 下线；后续可换 AgentScope Provider |

### 3.5 明确不迁移

| 域 | 原因 |
|----|------|
| `LegalDeterministicAuditEngine` | 无 LLM |
| `LegalAiOrchestrator` 编排骨架 | 仅换 Pipeline 内部运行时 |
| Flowable `LegalAiAuditDelegate` | 入口不变 |
| DB 表结构 | 业务数据与框架解耦 |

---

## 4. 迁移策略：Strangler + 双运行时

### 4.1 原则

1. **接口先行**：所有 LLM 调用经 `AiRuntimeFacade`，禁止新增 `org.springframework.ai` 直接 import。
2. **场景灰度**：配置项 `laby.ai.runtime=agentscope|spring-ai`（可按 tenant / scene 覆盖）。
3. **SSE 契约稳定**：前端 `contract-chat-panel.vue` 的 `eventType` 不变（content / tool_start / tool_end / proposal）。
4. **可观测先行**：Trace + StepLog 在 Middleware 统一落库，便于对比双运行时。

### 4.2 阶段划分

#### Phase 0 — 基座（2～3 周）

- 引入 Maven：`agentscope-harness`（锁定 2.0.x RC 具体版本，评审后固定）
- 新建 `laby-module-ai-runtime-api` + `laby-module-ai-runtime-agentscope`
- 实现 `AgentScopeModelAdapter`：至少 **OpenAI 兼容 / 通义 / DeepSeek**（与现网模型对齐）
- 从 `ai_model` + `ai_api_key` 读配置，**管理后台零改动**
- 单元测试：Model 连通性 + 单轮 completion

**验收：** 独立 main/test 可调通配置的模型；不影响现网 Spring AI。

#### Phase 1 — 法务 Agent（3～4 周）

- 8 个 `Legal*Tool` → AgentScope Tool（保留原有 Service 层）
- `LegalContractAgentServiceImpl` 重写为：
  - 构建 `ReActAgent`（Session + SessionKey = contractId + sessionId）
  - `streamEvents()` → 适配现有 `Flux<CommonResult<LegalContractChatRespVO>>`
- Middleware：
  - `TenantContextMiddleware`
  - `LegalAgentTraceMiddleware` → step log + trace
  - `LegalSseBridgeMiddleware` → tool_start/end
  - `ProposalPermissionMiddleware` → 对接 `LegalAgentProposalService`
- 配置开关：`legal.agent.runtime=agentscope`

**验收：** Agent 黄金路径（meta → paragraphs → opinions → knowledge）与 Spring AI 版对比；提案 Confirm 流程不变。

#### Phase 2 — 审核 Pipeline（2～3 周）

- `LegalAiAuditPipelineService` 改用 `BatchCompletionRunner`（**非 ReAct**，保持 JSON 数组输出）
- reasoning 流 → 继续写 `LegalAiAuditProgressHolder`（或改为 AgentScope event 转发）
- BPM 二轮审核回归

**验收：** E7 Playbook + 审核回归；Trace 完整；进度面板正常。

#### Phase 3 — 法务 Chat 普通模式 + SkillPack（1～2 周）

- 普通模式（非 Agent）走 `CompletionRunner` 或轻量 Agent（无 Tool）
- SkillPack 工具白名单映射 AgentScope ToolGroup

#### Phase 4 — 通用 AI 模块（4～6 周）

- `AiChatMessageServiceImpl` 迁移（含 RAG、MCP 决策）
- 知识库检索适配 `RetrievalAdapter`
- 写作/导图等按优先级逐个迁移或保留 spring-ai 孤岛

#### Phase 5 — 完全删除 Spring AI（2～3 周，**必做**）

- 图片 / 写作 / 导图 / MCP / RAG 全部切 AgentScope 或直连 SDK
- 删除 `laby-module-ai-runtime-springai` 整个模块
- 删除 `AiModelFactoryImpl`、`AiAutoConfiguration`、`AiUtils` 中 Spring AI 类型
- `laby-module-ai/pom.xml` 移除全部 `spring-ai-*` starter（约 14 个依赖）
- 验收：`mvn dependency:tree | grep springframework.ai` **无输出**

#### Phase 6（可选）— TinyFlow Provider 统一

- `getLLmProvider4Tinyflow` 从 agents-flex 改为 AgentScope ModelAdapter

**总工期（2 人全职，完全替换）：约 18～24 周**；3 人约 12～16 周。

---

## 5. 关键设计细节

### 5.1 模型配置复用

```yaml
# 新增（示例）
laby:
  ai:
    runtime: agentscope          # spring-ai | agentscope
    agentscope:
      session-store: redis       # redis | mysql | memory
      default-max-steps: 12
    scenes:
      legal-agent:
        runtime: agentscope
      legal-audit:
        runtime: agentscope
      ai-chat:
        runtime: spring-ai       # Phase 4 前
```

`AgentScopeModelAdapter` 读取逻辑：

```
ai_model (platform, model, key_id, temperature, max_tokens)
  → AgentScope ModelBuilder
ai_api_key (url, api_key)
  →  endpoint + credential
```

### 5.2 Tool 迁移规范

| 规则 | 说明 |
|------|------|
| Tool 只做适配 | 业务逻辑仍在 `LegalContractService` 等 Service |
| 上下文 | Middleware 在 Tool 执行前注入 `contractId/tenantId/userId` |
| 只读 vs 提案 | ToolGroup：`readonly` / `proposal`；提案 Tool 走 Permission=APPROVE |
| 命名 | 保留 `legal_get_contract_meta` 等 bean 名，便于 ToolRegistry 映射 |

### 5.3 SSE 事件映射

| AgentScope Event | 现有 SSE |
|------------------|----------|
| Text delta | `content` |
| Tool call start | `tool_start` |
| Tool result | `tool_end` |
| Permission request | `proposal`（扩展） |
| Error / Cancel | `error` |

前端 **Phase 1～3 可不改**；Phase 4 可选对齐 AG-UI 标准。

### 5.4 Session 与多租户

- SessionKey：`{tenantId}:{scene}:{businessId}:{sessionId}`
- 存储：Redis（与现网 Redis 共用实例，独立 key 前缀 `as:session:`）
- 滚动发布：AgentScope 2 支持同 sessionId 跨进程恢复（需验证 RC 稳定性）

### 5.5 批量审核（非 Agent）

审核 Pipeline **不应** 用 ReAct Agent（成本高、不稳定）。单独实现：

```
BatchCompletionRunner
  → 固定 Prompt 模板 + 段落批次 JSON
  → 单次 completion（可 stream reasoning）
  → 解析 LegalAiAuditOpinionItemBO 列表
```

与 AgentScope 共用 ModelAdapter，**不共用 ReAct 循环**。

---

## 6. 前端影响

| 页面 | Phase 1～3 | Phase 4+ |
|------|------------|----------|
| `contract-chat-panel.vue` | **无改动**（SSE 契约不变） | 可选增强事件 UI |
| `contract-audit-progress-panel.vue` | 无改动（仍轮询） | 可改 SSE 推送 |
| `views/ai/chat/` | 不动 | 随通用 chat 迁移 |
| `legal/ai-trace/` | 无改动 | 事件字段可能更丰富 |

---

## 7. 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| AgentScope Java 2.0 仍 RC | 高 | Phase 0 锁版本；保留 spring-ai 回滚适配器至 Phase 5 |
| 多平台 Model 适配工作量 | 高 | 优先 3 个主力平台；其余暂走 spring-ai 适配 |
| 双运行时维护成本 | 中 | 严格 `AiRuntimeFacade` 边界；Feature flag |
| MCP 与 AgentScope 集成 | 中 | Phase 4 再评估；chat 可延后 |
| 审核 JSON 解析回归 | 中 | 保留现有 golden tests + E7 评测 |
| 团队学习曲线 | 中 | Phase 0  POC + 内部 Workshop |

---

## 8. 验收标准（Go-Live）

### 8.1 Phase 1 法务 Agent

- [ ] 8 Tool 行为与 Spring AI 版一致（只读 6 + 提案 2）
- [ ] SSE tool 轨迹、提案 Confirm 正常
- [ ] `legal_agent_step_log` / `legal_ai_trace` 完整
- [ ] 租户隔离无串租

### 8.2 Phase 2 审核

- [ ] 首轮/二轮 BPM 审核通过
- [ ] Playbook 确定性 + LLM 批处理结果与现网 diff < 约定阈值
- [ ] 审核进度 UI 正常

### 8.3 Phase 5 全量

- [ ] 无 `org.springframework.ai` 编译依赖（test 除外）
- [ ] 性能：Agent P95 延迟 ≤ 现网 110%
- [ ] 回滚演练：10 分钟内切回 spring-ai

---

## 9. 团队与资源

| 角色 | 投入 |
|------|------|
| 后端（AgentScope 主程） | 1 人 × 全程 |
| 后端（法务域） | 0.5～1 人 × Phase 1～3 |
| 前端 | 0.2 人（联调 SSE） |
| QA | 0.5 人 × 每 Phase 末 |

---

## 10. 决策项（评审时请确认）

| # | 问题 | 建议默认 |
|---|------|----------|
| D1 | 是否完全去掉 Spring AI？ | **是**（已确认）；Phase 5 删 pom |
| D2 | 迁移顺序 | 法务 Agent → 审核 → 通用 Chat/RAG → 图片/MCP → 删 Spring AI |
| D3 | TinyFlow | Phase 6 可选；**不阻塞** Spring AI 下线 |
| D4 | Session 存储 | **Redis** |
| D5 | AgentScope 版本 | 锁定 **2.0.0-RC** 或 GA 后升级 |
| D6 | 过渡期双运行时 | 仅验收回滚，**Phase 5 前必须删** |

---

## 11. 相关文档

| 文档 | 关系 |
|------|------|
| `2026-06-03-legal-contract-agent-spec.md` | 原 Spring AI Agent 规格；Tool 列表仍有效 |
| `2026-06-04-legal-contract-platform-evolution-spec.md` | E4/E7/E8 Trace；编排边界仍有效 |
| AgentScope Java 2 文档 | https://java.agentscope.io/v2/en/docs/index.html |

---

## 12. 下一步

1. **评审本 Spec**（确认 D1～D6）
2. 编写 **Phase 0 实施计划**（`docs/superpowers/plans/2026-06-05-agentscope-phase0-foundation.md`）
3. 搭建 POC 分支：`feature/agentscope-poc`，仅 Model + 单 Tool Agent
4. Phase 0 通过后启动 Phase 1 法务 Agent 重写
