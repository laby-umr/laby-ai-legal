# AgentScope P0～P2 深化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or parallel windows per §8 of spec. Steps use checkbox syntax.

**Goal:** 接入 AgentScope Permission/HITL resume、Redis Session、MCP、Middleware 等 P0～P1 能力，P2 铺基础设施。

**Architecture:** 四窗口并行 — Session 基础设施、Permission+Resume、MCP、Enhance；共享 Config 文件由集成步骤合并。

**Tech Stack:** AgentScope 2.0.0-RC1, Spring Boot, Redis, Vue3 (web-ele)

**Spec:** `docs/superpowers/specs/2026-06-05-agentscope-p0-p2-deepening-spec.md`

---

## 并行窗口概览

| Window | Agent 任务 | 产出 |
|--------|-----------|------|
| W2 | P0 Redis Session | `session/*`, Properties, AutoConfiguration |
| W1 | P0 Permission + Resume | permission, resume service, API, SSE, 前端 |
| W3 | P1 MCP | `mcp/*`, AiChat 接线 |
| W4 | P1 Enhance | Middleware, sync agent, model retry |

---

## W2: P0 Redis Session

**Files:**
- Create: `laby-module-ai/.../agentscope/session/AgentScopeSessionKeyBuilder.java`
- Create: `laby-module-ai/.../agentscope/session/AgentScopeRedisSessionFactory.java`
- Modify: `AgentScopeProperties.java`, `AgentScopeAutoConfiguration.java`
- Modify: `LegalAgentScopeConfig.java` — `.session(...).sessionKey(...)` 
- Modify: `AiChatAgentScopeConfig.java` — 同上

- [ ] **Step 1:** 扩展 `AgentScopeProperties`：`sessionStore`（memory|redis）、`sessionTtlHours`
- [ ] **Step 2:** 实现 `AgentScopeRedisSessionFactory`：使用 AgentScope `RedisSession` 或 RC1 等价类 + Spring `StringRedisTemplate`
- [ ] **Step 3:** `AgentScopeAutoConfiguration` 暴露 `AgentScopeSessionFactory` Bean
- [ ] **Step 4:** `LegalAgentScopeConfig.buildAgent` 注入 session + key `legal:{contractId}:{sessionId}`
- [ ] **Step 5:** `AiChatAgentScopeConfig.buildAgent` 注入 session + key `chat:{conversationId}`
- [ ] **Step 6:** `application-local.yaml` 示例配置
- [ ] **Step 7:** `mvn -pl laby-module-ai,laby-module-legal -am compile`

---

## W1: P0 Permission + HITL Resume

**Files:**
- Create: `laby-module-legal/.../permission/LegalAgentPermissionContextFactory.java`
- Create: `laby-module-legal/.../middleware/LegalAgentConfirmSseMiddleware.java`
- Create: `laby-module-legal/.../service/agent/LegalContractAgentResumeService.java`
- Create: `laby-module-legal/.../controller/.../LegalContractAgentConfirmReqVO.java`
- Modify: `LegalAgentScopeConfig.java` — `.permissionContext(...)`
- Modify: `LegalContractAgentServiceImpl.java` — 映射 `RequireUserConfirmEvent` → SSE
- Modify: `LegalContractAgentController.java` — `POST /confirm`
- Modify: `LegalProposeAdoptOpinionTool.java`, `LegalProposeSkipParagraphTool.java` — 配合 ASK（Tool 内仍 createProposal 审计）
- Modify: `contract-chat-panel.vue`, `api/legal/contract/index.ts`

- [ ] **Step 1:** `LegalAgentPermissionContextFactory.build(allowProposal)` — ASK for propose tools
- [ ] **Step 2:** `LegalAgentConfirmSseMiddleware` — onActing 前若 ASK 事件则 push confirm_required
- [ ] **Step 3:** `LegalContractAgentServiceImpl.mapAgentEventToSse` 处理 confirm 相关 AgentEvent
- [ ] **Step 4:** `LegalContractAgentResumeService.resume(sessionId, confirmId, approved)` — ConfirmResult + agent resume
- [ ] **Step 5:** Controller + VO
- [ ] **Step 6:** 前端：confirm_required 显示确认 UI；调用 `/confirm` 而非仅 execute
- [ ] **Step 7:** compile

---

## W3: P1 MCP Client

**Files:**
- Create: `laby-module-ai/.../agentscope/mcp/AgentScopeMcpToolRegistrar.java`
- Modify: `AiChatToolRegistry.java` — 接受 mcpClientNames
- Modify: `AiChatMessageServiceImpl.java` — 读 role.mcpClientNames，注册 MCP tools，移除 warn-only

- [ ] **Step 1:** 调研 RC1 MCP API（Workspace MCP 或 McpClient builder）
- [ ] **Step 2:** `AgentScopeMcpToolRegistrar.register(toolkit, clientNames)` 
- [ ] **Step 3:** `buildChatHarnessAgent` 合并 MCP tools
- [ ] **Step 4:** 单 MCP 失败 warn 继续
- [ ] **Step 5:** compile

---

## W4: P1 Enhance

**Files:**
- Create: `laby-module-ai/.../chat/middleware/AiChatTraceMiddleware.java`
- Modify: `AiChatAgentScopeConfig.java` — 注册 middleware
- Modify: `LegalContractChatServiceImpl.chat()` — agentMode 时 `contractAgentService.runSync` 或 HarnessAgent.call
- Modify: `AgentScopeModelFactory.java` — maxRetries
- Modify: `AgentScopeProperties.java` — modelMaxRetries

- [ ] **Step 1:** AiChatTraceMiddleware — log tool start/end
- [ ] **Step 2:** AiChatAgentScopeConfig 注册 middleware
- [ ] **Step 3:** 法务 sync chat 提取 `runSync` 到 LegalContractAgentService
- [ ] **Step 4:** Model factory retry
- [ ] **Step 5:** compile

---

## W5: P2（后续迭代）

- SkillPack → workspace/skills writer
- Sub-agent 审核试点
- spring.ai.* → laby.ai.* 配置迁移

---

## 集成检查清单

- [ ] `LegalAgentScopeConfig` 同时含 permission + session（无冲突）
- [ ] SSE 黄金路径：问答 → propose → confirm → resume → 完成
- [ ] MCP 角色对话 smoke test
- [ ] 全量 compile
