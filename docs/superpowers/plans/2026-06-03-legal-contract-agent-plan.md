# 法务合同 Agent Phase 1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or parallel windows per §8 of spec.
> **Spec:** [2026-06-03-legal-contract-agent-spec.md](../specs/2026-06-03-legal-contract-agent-spec.md)

**Goal:** 交付 Phase 1 只读 Agent MVP：4 Tool、Agent 模式、SSE Tool 轨迹、前端 Switch。

**Architecture:** Spring AI ToolCallback + ToolContext(CONTRACT_ID) + LegalContractChatServiceImpl 分支；Tool 事件经 Aspect 推送 SSE。

**Tech Stack:** Spring AI 1.1.5, Apache POI(已有), Vue3, Element Plus, MySQL

---

## 并行窗口分配

| 窗口 | 负责人 | 完成后产出 |
|------|--------|------------|
| W1 | Agent-1 | `legal/tool/agent/*` 4 个 Tool + Context + Aspect |
| W4 | Agent-2 | SQL 种子 + `LegalAiChatRoleConstants` 扩展 |
| W2 | Agent-3 | `LegalAgentToolProvider` + ChatService 改造 + VO |
| W3 | Agent-4 | 前端 Agent 模式 + tool 轨迹 |

**集成（主会话）：** W1+W4 完成后 W2，再 W3，`mvn compile` 联调。

---

### Task W1: Tool 基础设施与四个只读 Tool

**Files:**
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalAgentToolContext.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalAgentToolSupport.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalAgentToolAspect.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalAgentSseEventHolder.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalGetContractMetaTool.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalSearchParagraphsTool.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalGetAuditOpinionsTool.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/tool/agent/LegalSearchKnowledgeTool.java`

- [ ] **Step 1:** `LegalAgentToolContext` 常量 CONTRACT_ID, READONLY, SESSION_ID
- [ ] **Step 2:** `LegalAgentToolSupport` 提供 `requireContractId(ToolContext)`, `requireReadonly(ToolContext)`
- [ ] **Step 3:** `LegalAgentSseEventHolder` ThreadLocal 队列 + `pollEvents()` / `pushToolStart` / `pushToolEnd`
- [ ] **Step 4:** `LegalAgentToolAspect` `@Around` `com.laby.module.legal.tool.agent..*.apply(..)` 记录 start/end + summary
- [ ] **Step 5:** 实现 4 个 Tool（参考 `UserProfileQueryToolFunction` 模式，`@Component("legal_xxx")`）
- [ ] **Step 6:** 不修改 ChatService（W2 负责）

---

### Task W4: SQL 与角色常量

**Files:**
- Create: `sql/mysql/laby-legal-agent-phase1.sql`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/enums/LegalAiChatRoleConstants.java`

- [ ] **Step 1:** SQL 插入 4 条 `ai_tool`（INSERT IGNORE 或 ON DUPLICATE KEY name）
- [ ] **Step 2:** SQL 插入 1 条 `ai_chat_role`「法务合同问答 Agent」，tool_ids 引用 4 tool
- [ ] **Step 3:** 扩展 `LegalAiChatRoleConstants` 增加 `ROLE_NAME_QA_AGENT` + `DEFAULT_SYSTEM_MESSAGE_QA_AGENT`

---

### Task W2: Chat 服务集成

**Files:**
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/agent/LegalAgentToolProvider.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalContractChatServiceImpl.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/controller/admin/contract/vo/LegalContractChatReqVO.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/controller/admin/contract/vo/LegalContractChatRespVO.java`

- [ ] **Step 1:** ReqVO 增加 `agentMode`, `sessionId`；RespVO 增加 `eventType`, `toolName`, `toolSummary`, `sessionId`
- [ ] **Step 2:** `LegalAgentToolProvider.getReadOnlyToolCallbacks()` + `buildToolContext(contractId, sessionId)`
- [ ] **Step 3:** `buildPrompt` 分支：agentMode 时不调用 buildContractContext / buildChatKnowledgeContext
- [ ] **Step 4:** `chatStream` agentMode 时挂 toolCallbacks；merge `LegalAgentSseEventHolder.pollEvents()` 到 Flux
- [ ] **Step 5:** 依赖 W1 的类（若 W1 未完成，先 stub）

---

### Task W3: 前端

**Files:**
- Modify: `laby-ui/laby-ui-admin-vben/apps/web-ele/src/views/legal/contract/components/contract-chat-panel.vue`
- Modify: `laby-ui/laby-ui-admin-vben/apps/web-ele/src/api/legal/contract/index.ts`

- [ ] **Step 1:** API 类型增加 `agentMode`, `sessionId`；Resp 增加 tool 事件字段
- [ ] **Step 2:** Agent 模式 ElSwitch + localStorage
- [ ] **Step 3:** 每次问答生成 sessionId (uuid)
- [ ] **Step 4:** SSE 解析 tool_start/tool_end，渲染 tool 轨迹 UI
- [ ] **Step 5:** 请求携带 agentMode

---

### Task INT: 集成验证

- [ ] `mvn compile -pl laby-module-legal -am -DskipTests` SUCCESS
- [ ] 检查 linter

---

## Phase 2 任务预览（本计划不执行）

- ~~Tool: `legal_get_audit_report`, `legal_compare_audit_rounds`~~ ✅ 见 `laby-legal-agent-phase2.sql`
- ~~表: `legal_agent_step_log` + Controller~~ ✅
- Agent 模式彻底瘦 Prompt（已在 Phase 1 实现，无需大段上下文）
- ~~管理端菜单 + 列表页~~ ✅ `legal/agent-log/index`

## Phase 3 任务预览（本计划不执行）

- `LegalContractAgentService`
- Proposal Tool + Confirm API
- 段落向量索引
