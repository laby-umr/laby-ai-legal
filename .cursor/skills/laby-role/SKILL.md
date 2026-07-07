---
name: laby-role
description: >-
  Business ROLE selection for laby-admin development. Pick legal-backend, ai-backend, or frontend
  role based on task; each role adds domain focus on top of laby-global + laby-project. Use when
  implementing features in a specific business area.
---

# 业务角色（开发视角）

**性质：** 做业务时 **戴哪顶帽子** — 同一套全局规范 + 项目结构，**关注点不同**。  
**用法：** 任务开始时选 **一个主角色**；跨模块改动以主角色为准，列清单覆盖其它模块。

**叠加顺序：** 用户指令 → **`laby-global`** → **`laby-project`** → **本 Skill 选中角色章节**

**Announce:** "Using laby-role as [legal|ai|frontend]."

---

## 角色选择

| 主改动 | 角色 | 模块 |
|--------|------|------|
| 合同、审核、OnlyOffice、BPM | **legal-backend** | `laby-module-legal` |
| Chat、RAG、Agent、知识库 | **ai-backend** | `laby-module-ai` |
| 管理端页面、API 封装 | **frontend** | `laby-ui` |
| 多模块 | 按 **主业务** 选一个 | 手动列出次模块检查项 |

纯框架/infra 改动：不强制角色，用 `laby-project` + `laby-global` 即可。

---

## 角色：legal-backend

**领域：** 合同全生命周期 — 上传、解析、AI 审核、意见、OnlyOffice、BPM。

**状态机：**

```
DRAFT → PARSING → PARSED → AI_AUDITING → AI_AUDITED → (BPM…)
```

**架构要点：**

| 组件 | 职责 |
|------|------|
| `LegalAuditKernel` | 正式/预览审核编排 |
| `LegalAuditKernelCommand` | `@Builder` 跨层命令 |
| `LegalAiAuditPipelineService` | LLM 分批 |
| `LegalContractProcessStarter` | afterCommit 启动管道 |

**异步：** 事务内短 DB；`afterCommit` 再解析/审核；二轮用 Stream Consumer，勿与首轮管道混淆。

**禁止：** Service 直接 new HarnessAgent → 用 `AiLlmClient` / `LegalAgentScopeConfig`。

**验证：** Smoke `GET /admin-api/legal/contract/page`；E2E 上传→解析→审核→意见入库。

---

## 角色：ai-backend

**领域：** 对话、知识库、文档解析、向量检索、Agent 工具链。

**RAG 链路（默认勿改）：**

```
Query → Intent → Dense + Sparse(FULLTEXT) → RRF(k=60) → Rerank
```

**分层：** Service 用 `AiLlmClient` 门面；AgentScope 只在 `framework/agentscope`。

**配置：** 新开关进 `AgentScopeProperties`，不散落 `@Value`。

**验证：** Smoke `GET /admin-api/ai/knowledge/page`；E2E 上传文档→分段→召回诊断。

---

## 角色：frontend

**领域：** Vben + Element Plus 管理端。

**目录：** `laby-ui/apps/web-ele/src/api|views/{module}`

**约定：**

- TS 类型与后端 VO **同名**
- 状态展示用 `DICT_TYPE`，禁止硬编码文案
- OnlyOffice iframe、Chat SSE 见 `laby-project` nginx 配置
- 不提交 `console.log`；复杂逻辑抽 composable

**验证：** 路由可访问、主流程可点、console 无 error；API 变更配合 workflow 更新 Postman。

---

## 角色协作（常见跨角色任务）

| 任务 | 主角色 | 次角色关注 |
|------|--------|------------|
| 合同审核页 | frontend | legal API、字典 |
| 知识库召回 UI | frontend | ai 诊断字段 |
| 新审核 API | legal-backend | frontend API 类型 + Postman |

## 详细清单

见 [reference.md](reference.md)
