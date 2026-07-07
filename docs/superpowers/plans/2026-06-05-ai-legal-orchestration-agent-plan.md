# AI 法务编排 Agent 实施计划

> 对应规格：`docs/superpowers/specs/2026-06-05-ai-legal-orchestration-agent-spec.md`  
> 日期：2026-06-05

---

## Phase 1：编排基础（当前执行）

### 1.1 数据库

- [x] `sql/mysql/laby-legal-orchestration-phase1.sql`
  - `legal_orchestration_session`
  - `legal_orchestration_file_item`
  - ALTER `legal_agent_proposal` / `legal_contract`
  - 字典初始化
  - `ai_tool` + `ai_chat_role` 种子

### 1.2 后端基础

- [x] 枚举：`LegalContractCreateSourceEnum`、`LegalOrchestrationPhaseEnum`、`LegalOrchestrationFileItemStatusEnum`
- [x] 扩展 `LegalAgentProposalActionEnum`
- [x] `LegalDictTypeConstants`、`LegalOrchestrationConstants`
- [x] `ErrorCodeConstants` 编排段
- [x] DO / Mapper / Service：编排会话、文件项
- [x] 扩展 `LegalAgentProposalDO` + `LegalAgentProposalService`（编排提案创建与执行）
- [x] `LegalOrchestrationClassificationService`（LLM 分类）
- [x] `LegalOrchestrationContractCreateExecutor`（批量创建）

### 1.3 Tool

- [x] `legal_orchestration_list_contract_types`
- [x] `legal_orchestration_register_files`
- [x] `legal_orchestration_propose_file_classification`
- [x] `legal_orchestration_propose_create_contracts`
- [x] `LegalOrchestrationToolRuntimeContext` + Middleware
- [x] `AiChatHarnessSupport` 扩展点（ai 模块）+ Legal 实现

### 1.4 接口

- [x] `LegalOrchestrationProposalController`

### 1.5 前端

- [x] `#/api/legal/orchestration`
- [x] `legal-orchestration-proposal-card.vue`
- [x] 接入 `ai/chat/index`

### 1.6 验证

```bash
mvn -pl laby-module-legal -am compile -DskipTests
```

---

## Phase 2：类型包与查询（已完成）

- [x] `legal_orchestration_propose_type_package`
- [x] `legal_orchestration_list_user_contracts`
- [x] `legal_orchestration_get_contract_summary`
- [x] `legal_orchestration_register_latest_attachments`
- [x] 用户私有类型包表 `legal_orchestration_type_package_draft`
- [x] `GET /legal/orchestration/session/get`

---

## Phase 3：体验与权限（已完成）

- [x] 提案创建时 SSE 事件推送（`LegalAgentSseEventHolder`）
- [x] 菜单与权限 `legal:orchestration:query|execute`
- [x] 对话内编排卡片：阶段 + 已创建合同跳转
- [x] 联调脚本 `docs/superpowers/scripts/orchestration-smoke.ps1`
