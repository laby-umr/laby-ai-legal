# Spec：AgentScope 2.0 能力深化（P0～P2）

| 属性 | 值 |
|------|-----|
| 版本 | v1.0 |
| 日期 | 2026-06-05 |
| 状态 | **已评审 — 可并行实施** |
| 前置 | [`2026-06-05-laby-module-ai-agentscope-inplace-refactor-spec.md`](./2026-06-05-laby-module-ai-agentscope-inplace-refactor-spec.md)（Spring AI 已移除） |
| 运行时 | AgentScope Java **2.0.0-RC1**（`agentscope-harness`） |

---

## 1. 背景

Spring AI 原位替换已完成：LLM/Agent 主链路已走 `AgentScopeLlmClient` + `HarnessAgent`。但 AgentScope 2.0 **Harness 工程化能力**利用率低：

- 写操作提案走自研 `LegalAgentProposalService` + SSE，**未**用 Permission ASK + HITL resume
- `HarnessAgent` **未**接 Redis Session，多实例/重启丢 Agent 内存态
- 角色 `mcpClientNames` 仅 warn，MCP 未接入
- 通用 AI Chat **无** Middleware；法务同步 `chat()` 不走 Tool 循环
- SkillPack、Sub-agent、Context Compaction 未启用

本 Spec 分 **P0 / P1 / P2** 三档，按业务价值排序，**工作流之间文件隔离，可多窗口并行**。

---

## 2. 目标（Must / Should）

### P0 — 生产必做

| ID | 目标 |
|----|------|
| P0-1 | 法务写操作 Tool 接入 AgentScope **Permission**（`legal_propose_*` → ASK） |
| P0-2 | 用户 Confirm/Cancel 后通过 **ConfirmResult** **resume** Agent，而非仅 DB 执行后 Agent 已结束 |
| P0-3 | SSE 兼容：保留 `eventType: proposal`；新增 `eventType: confirm_required`（含 `confirmId`） |
| P0-4 | `HarnessAgent` 接入 **Redis Session**（key：`laby.ai.agentscope.session-key-prefix` + sessionId） |
| P0-5 | 保留现有 `legal_agent_proposal` 表作审计；执行仍经 `LegalAgentProposalService` |

### P1 — 能力补齐

| ID | 目标 |
|----|------|
| P1-1 | AI Chat 角色 `mcpClientNames` → AgentScope **MCP Client** 注册进 Toolkit |
| P1-2 | 通用 AI Chat 增加 **Trace Middleware**（对齐法务 `LegalAgentTraceMiddleware` 思路） |
| P1-3 | 法务 **同步** `chat()` 在 `agentMode=true` 时走 `HarnessAgent.call()` |
| P1-4 | `AgentScopeModelFactory` 支持 **maxRetries + fallbackModel**（配置可选） |
| P1-5 | 长对话 **Context Compaction**（Harness 内置或 Middleware 触发阈值） |

### P2 — 架构演进

| ID | 目标 |
|----|------|
| P2-1 | SkillPack 工具/提示词 → `workspace/skills/` **Dynamic Skills**（法务 CHAT/AUDIT 场景） |
| P2-2 | 审核 Orchestrator 可选 **Sub-agent**（条款分类 / 批审 / 报告，只读 Tool 子集） |
| P2-3 | Chat 附件 **多模态 DataBlock**（图片 URL/base64） |
| P2-4 | 配置前缀 `spring.ai.*` → `laby.ai.*`（vectorstore、rerank、MCP 占位） |

---

## 3. 非目标（Won't）

| # | 说明 |
|---|------|
| N1 | 不用 AgentScope RAG API（RC deprecated，继续 Qdrant + HttpAiEmbeddingClient） |
| N2 | TinyFlow/agents-flex 迁移（单独 Spec） |
| N3 | Sandbox/Docker 代码执行 |
| N4 | 改 REST 路径；破坏现有 SSE `content/tool_*` 事件 |

---

## 4. 现状与差距

### 4.1 提案链路（Today）

```
LLM → legal_propose_* Tool → LegalAgentProposalService.create* → DB + SSE proposal
用户点确认 → POST /legal/contract/agent/proposal/execute → 直接写库
Agent 流可能已结束，无 resume
```

### 4.2 目标链路（Target）

```
LLM 调用 legal_propose_* 
  → PermissionEngine ASK 
  → streamEvents 发出 RequireUserConfirmEvent 
  → 映射 SSE confirm_required + 创建 proposal 审计记录
用户 Confirm 
  → POST .../agent/confirm { confirmId, approved, proposalNo? }
  → ConfirmResult → agent.resume(confirmResult) 
  → Tool 返回「用户已确认」→ Agent 继续推理
用户 Cancel → ConfirmResult(deny) → Tool 返回取消 → Agent 继续
Execute 写库仍由 LegalAgentProposalService（Permission Middleware 或 Tool 内调用）
```

### 4.3 Session（Today vs Target）

| 项 | Today | Target |
|----|-------|--------|
| Agent 状态 | 仅 Workspace 目录 | Redis Session 持久化 |
| 多实例 | `LegalAgentSessionGuard` 进程内互斥 | Redis 分布式锁 + Session |
| 配置 | `workspace-path` | + `session-store: redis` |

---

## 5. 架构设计

### 5.1 模块职责

```
laby-module-ai/framework/agentscope/
  session/AgentScopeRedisSessionFactory.java    # P0-4
  session/AgentScopeSessionKeyBuilder.java
  mcp/AgentScopeMcpToolRegistrar.java           # P1-1
  chat/middleware/AiChatTraceMiddleware.java    # P1-2
  model/AgentScopeModelFactory.java             # P1-4 fallback

laby-module-legal/framework/agentscope/
  permission/LegalAgentPermissionContextFactory.java  # P0-1
  middleware/LegalAgentConfirmSseMiddleware.java        # P0-2 SSE bridge
  skill/LegalSkillPackSkillWriter.java                # P2-1

laby-module-legal/service/agent/
  LegalContractAgentResumeService.java          # P0-2 resume 编排
  LegalContractAgentServiceImpl.java            # 事件映射 + resume 入口
```

### 5.2 Permission 规则（法务 CHAT Agent）

| Tool | allowProposal=false | allowProposal=true |
|------|---------------------|---------------------|
| `legal_propose_adopt_opinion` | DENY（未注册） | **ASK** |
| `legal_propose_skip_paragraph` | DENY | **ASK** |
| 只读 Tool（8 个中 6 个） | ALLOW | ALLOW |

`HarnessAgent.builder().permissionContext(...)` 在 `LegalAgentScopeConfig.buildAgent()` 注入。

### 5.3 Redis Session Key

```
{sessionKeyPrefix}{scope}:{sessionId}
scope = legal:chat:{contractId} | ai:chat:{conversationId}
TTL = 24h（可配置 laby.ai.agentscope.session-ttl-hours）
```

### 5.4 SSE 事件扩展（向后兼容）

| eventType | 字段 | 说明 |
|-----------|------|------|
| `proposal` | proposalNo, proposalAction, ... | **保留**（审计 + UI 卡片） |
| `confirm_required` | confirmId, toolName, summary | **新增**（Agent 暂停点） |
| `content` / `tool_*` / `error` | 不变 | |

### 5.5 MCP（P1）

- 读 `AiChatRoleDO.mcpClientNames`（逗号分隔）
- 通过 AgentScope Workspace MCP 注册或 `McpClientManager` 等价 API 将 remote tools 并入 `Toolkit`
- 失败单条 warn，不阻断对话

---

## 6. API 变更

### 6.1 新增

```
POST /admin-api/legal/contract/agent/confirm
Body: { sessionId, confirmId, approved: boolean, proposalNo?: string }
```

- `approved=true`：写 ConfirmResult ALLOW + 可选触发 executeProposal
- `approved=false`：ConfirmResult DENY + cancelProposal

### 6.2 保留

```
POST /legal/contract/agent/proposal/execute
POST /legal/contract/agent/proposal/cancel
```

兼容旧前端；新前端优先走 `/confirm` 一体化。

---

## 7. 配置

```yaml
laby:
  ai:
    agentscope:
      workspace-path: /data/laby-agentscope
      session-store: redis          # P0-4
      session-key-prefix: "as:"
      session-ttl-hours: 24
      default-max-steps: 12
      model-max-retries: 2          # P1-4
      compaction-token-threshold: 120000  # P1-5，0=关闭
    vector-store:                   # P2-4 迁移目标前缀
      qdrant:
        host: 127.0.0.1
        port: 6334
```

---

## 8. 并行工作流（多窗口）

| 窗口 | 范围 | 独占新建文件 | 共享文件（最后集成） |
|------|------|--------------|----------------------|
| **W1 P0-Permission** | Permission + Confirm SSE + Resume API + 前端 confirm | `permission/*`, `LegalContractAgentResumeService`, `LegalAgentConfirmSseMiddleware` | `LegalAgentScopeConfig`, `LegalContractAgentServiceImpl`, `contract-chat-panel.vue` |
| **W2 P0-Session** | Redis Session 工厂 + 配置 | `session/*` | `LegalAgentScopeConfig`, `AiChatAgentScopeConfig`, `AgentScopeProperties` |
| **W3 P1-MCP** | MCP 注册 | `mcp/*` | `AiChatMessageServiceImpl`, `AiChatToolRegistry` |
| **W4 P1-Enhance** | Chat Middleware + 同步 Agent + Model retry | `chat/middleware/*` | `LegalContractChatServiceImpl`, `AgentScopeModelFactory` |
| **W5 P2**（后续） | Skills / Sub-agent / 配置迁移 | `skill/*` | SkillPack 服务 |

**集成顺序**：W2 → W1 → W3/W4（W1/W2 都改 Config 时需人工 merge 或串行 Config 段）。

---

## 9. 测试策略

| 层级 | 内容 |
|------|------|
| 单元 | Permission 规则、SessionKey 构建、Confirm 映射 |
| 集成 | `LegalContractAgentResumeServiceIT`：mock Confirm → resume |
| 手工 | Agent 提议采纳 → confirm_required → 确认 → Agent 继续输出 |
| 门禁 | `mvn -pl laby-module-ai,laby-module-legal -am compile` |

---

## 10. 风险与回退

| 风险 | 缓解 |
|------|------|
| RC1 Permission/Resume API 变更 | 封装 `LegalAgentPermissionContextFactory` 单点适配 |
| Redis 不可用 | `session-store: memory` 降级 |
| 双轨 proposal + confirm 前端混乱 | 两事件都发；UI 以 confirm_required 驱动，proposal 作卡片数据 |

---

## 11. 验收标准

### P0 Done

- [ ] `legal_propose_*` 触发 ASK，流式出现 `confirm_required`
- [ ] Confirm 后 Agent **同 session** 继续推理（非新请求）
- [ ] Redis 存在 session key；重启后同 sessionId 可恢复（至少消息态）
- [ ] 现有 proposal 执行/取消 API 仍可用

### P1 Done

- [ ] 角色配置 MCP 后 Chat 可调用 MCP Tool
- [ ] AI Chat 有 tool 轨迹日志
- [ ] 法务同步 agentMode 可触发 Tool
- [ ] 主模型失败自动 retry/fallback（可配置）

### P2 Done

- [ ] SkillPack 变更反映到 workspace/skills
- [ ] 至少 1 个审核 Sub-agent 试点
- [ ] `laby.ai.vector-store` 配置生效

---

## 12. 参考

- [AgentScope Permission System](https://java.agentscope.io/v2/en/docs/building-blocks/permission-system.html)
- [AgentScope 2.0 Harness](https://java.agentscope.io/v2/en/docs/index.html)
- 现有：`LegalAgentProposalService`, `LegalAgentScopeConfig`, `contract-chat-panel.vue`
