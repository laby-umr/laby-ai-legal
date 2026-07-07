# AI 法务编排 Agent 企业级规格说明

> 版本：1.0  
> 日期：2026-06-05  
> 状态：Phase 1 实施中  
> 对齐：ruoyi-pro / laby 代码风格、阿里 P3C、字典/枚举/常量/异常统一管理

---

## 1. 背景与目标

### 1.1 问题

当前法务能力分散在三处：

| 能力域 | 入口 | 局限 |
|--------|------|------|
| AI 一审/二审 | 创建合同后 Pipeline | 需人工先建合同 |
| 合同页 Agent | `review.vue` 侧栏/Tab | 绑定单合同，偏快操作 |
| 全局 AI 对话 | `ai/chat` | 无法完成「上传→分类→确认→落库→跟踪」编排 |

用户期望：**全局对话中由 Agent 完成语义理解、逐步确认、再写库**，而非「上传即创建」。

### 1.2 目标

1. 在 **AI 聊天「法务编排」角色** 中，实现 Plan → Confirm → Execute 闭环。
2. 写库操作一律经 `legal_agent_proposal` 提案，用户确认后执行。
3. 合同 `create_source` 区分 `MANUAL` / `AI_CHAT`，支持按用户/对话追溯。
4. 表设计、字典、枚举、常量、错误码符合 laby 模块规范。

### 1.3 非目标（本期）

- Subagent / MCP 扩展（预留接口，不实现）
- OnlyOffice 写回 Word（独立演进）
- 类型包自动发布为租户正式配置（Phase 2，默认用户私有草稿）

---

## 2. 角色与状态机

### 2.1 Agent 分工

```
┌─────────────────────────────────────────────────────────────┐
│ 全局法务编排 Agent（ai_chat_role: 法务编排助手）              │
│  语义分类 → 映射确认 → 缺类型起草 → 多轮修订 → 确认落库      │
└─────────────────────────────────────────────────────────────┘
         │                              │
         ▼                              ▼
┌──────────────────┐          ┌──────────────────────┐
│ 合同页 Agent      │          │ Pipeline 一审/二审    │
│ 单合同快操作      │          │ 创建后自动审核        │
└──────────────────┘          └──────────────────────┘
```

### 2.2 编排阶段（`LegalOrchestrationPhaseEnum`）

| 阶段 | 编码 | 说明 |
|------|------|------|
| 初始化 | `INIT` | 会话创建，等待用户意图 |
| 文件登记 | `FILE_REGISTERED` | 附件已登记，待分类 |
| 分类待确认 | `CLASSIFY_PENDING` | 已生成分类提案 |
| 分类已确认 | `CLASSIFY_CONFIRMED` | 用户确认类型映射 |
| 类型包起草 | `TYPE_PACKAGE_DRAFTING` | 缺类型，AI 起草条款/规则 |
| 类型包待确认 | `TYPE_PACKAGE_PENDING` | 类型包提案待确认 |
| 创建待确认 | `CREATE_PENDING` | 批量创建合同提案待确认 |
| 已落库 | `CONTRACTS_CREATED` | 合同已创建并进入 Pipeline |
| 跟踪查询 | `TRACKING` | 按用户/时间查进度、风险、下载 |
| 已结束 | `CLOSED` | 会话归档 |

状态迁移：**仅 Tool `propose_*` 可推进至 PENDING；`execute_*`（用户确认）后进入下一阶段**。

### 2.3 提案状态机（复用 `LegalAgentProposalStatusEnum`）

`PENDING` → `EXECUTED` | `CANCELLED` | `EXPIRED`

---

## 3. 数据模型

### 3.1 新增表

#### `legal_orchestration_session`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| conversation_id | BIGINT | `ai_chat_conversation.id` |
| user_id | BIGINT | 发起人 |
| phase | VARCHAR(32) | 编排阶段枚举 |
| model_id | BIGINT | 可选，绑定模型 |
| remark | VARCHAR(512) | 备注 |
| 标准审计字段 | | creator/create_time/.../tenant_id |

索引：`uk_conversation`（conversation_id + deleted）、`idx_user_phase`

#### `legal_orchestration_file_item`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| session_id | BIGINT | 编排会话 |
| infra_file_id | BIGINT | 附件 |
| file_name | VARCHAR(255) | 冗余文件名 |
| status | VARCHAR(32) | REGISTERED/CLASSIFIED/MAPPED/CREATED |
| suggested_type_id | BIGINT | AI 建议类型 |
| confirmed_type_id | BIGINT | 用户确认类型 |
| contract_id | BIGINT | 落库后回填 |
| sort | INT | 排序 |

### 3.2 扩展表

#### `legal_agent_proposal`

| 变更 | 说明 |
|------|------|
| `conversation_id` BIGINT NULL | 全局编排会话 |
| `contract_id` 改为 NULL | 编排类提案可无合同 |
| `action` 扩展 | 见 §4.2 |

#### `legal_contract`

| 字段 | 类型 | 字典/枚举 |
|------|------|-----------|
| create_source | VARCHAR(32) | `legal_contract_create_source`：MANUAL / AI_CHAT |
| create_conversation_id | BIGINT NULL | AI 对话来源 |

### 3.3 字典（`system_dict_type` / `system_dict_data`）

| dict_type | 值 | 标签 |
|-----------|-----|------|
| legal_contract_create_source | MANUAL | 人工创建 |
| legal_contract_create_source | AI_CHAT | AI 对话创建 |
| legal_orchestration_phase | INIT | 初始化 |
| legal_orchestration_phase | FILE_REGISTERED | 文件已登记 |
| legal_orchestration_phase | CLASSIFY_PENDING | 分类待确认 |
| legal_orchestration_phase | CLASSIFY_CONFIRMED | 分类已确认 |
| legal_orchestration_phase | TYPE_PACKAGE_DRAFTING | 类型包起草中 |
| legal_orchestration_phase | TYPE_PACKAGE_PENDING | 类型包待确认 |
| legal_orchestration_phase | CREATE_PENDING | 创建待确认 |
| legal_orchestration_phase | CONTRACTS_CREATED | 已创建合同 |
| legal_orchestration_phase | TRACKING | 跟踪查询 |
| legal_orchestration_phase | CLOSED | 已结束 |
| legal_orchestration_file_status | REGISTERED | 已登记 |
| legal_orchestration_file_status | CLASSIFIED | 已分类 |
| legal_orchestration_file_status | MAPPED | 已映射 |
| legal_orchestration_file_status | CREATED | 已落库 |

---

## 4. 枚举 / 常量 / 错误码

### 4.1 枚举（Java，`laby-module-legal/enums`）

| 类 | 用途 |
|----|------|
| `LegalContractCreateSourceEnum` | 合同创建来源 |
| `LegalOrchestrationPhaseEnum` | 编排阶段 |
| `LegalOrchestrationFileItemStatusEnum` | 文件项状态 |
| `LegalAgentProposalActionEnum`（扩展） | 提案动作 |
| `LegalDictTypeConstants` | 字典 type 常量 |

### 4.2 提案动作扩展（`LegalAgentProposalActionEnum`）

| action | 写库 | 说明 |
|--------|------|------|
| `CLASSIFY_CONFIRM` | 否 | 确认文件→类型映射 |
| `CREATE_TYPE_PACKAGE` | 是 | 创建用户私有类型包草稿（Phase 2） |
| `CREATE_CONTRACTS_BATCH` | 是 | 批量创建合同并启动 Pipeline |
| `ADOPT_OPINION` | 是 | 已有（合同页） |
| `SKIP_PARAGRAPH` | 是 | 已有（合同页） |

### 4.3 常量（`LegalOrchestrationConstants`）

- `ORCHESTRATION_ROLE_CODE = legal_orchestration`
- `ORCHESTRATION_TOOL_PREFIX = legal_orchestration_`
- `PROPOSAL_TTL_MINUTES = 30`
- `DEFAULT_AUDIT_LEVEL = STANDARD`
- `DEFAULT_PARTY_ROLE = A`

### 4.4 错误码（`ErrorCodeConstants`，段 `1_050_000_*`）

| 码 | 常量 | 说明 |
|----|------|------|
| 1_050_000_040 | ORCHESTRATION_SESSION_NOT_EXISTS | 编排会话不存在 |
| 1_050_000_041 | ORCHESTRATION_FILE_NOT_EXISTS | 编排文件项不存在 |
| 1_050_000_042 | ORCHESTRATION_PHASE_INVALID | 当前阶段不允许该操作 |
| 1_050_000_043 | ORCHESTRATION_CLASSIFY_EMPTY | 无待分类文件 |
| 1_050_000_044 | ORCHESTRATION_TYPE_NOT_RESOLVED | 类型未确认 |
| 1_050_000_045 | ORCHESTRATION_PROPOSAL_EXPIRED | 编排提案已过期 |

---

## 5. Tool API（Agent 工具）

命名前缀：`legal_orchestration_`，注册为 Spring Bean + `ai_tool` 记录。

### 5.1 只读

| Tool | 说明 |
|------|------|
| `legal_orchestration_list_contract_types` | 列出租户可用合同类型（id/code/name） |
| `legal_orchestration_list_user_contracts` | 按时间/状态查当前用户合同（Phase 2） |
| `legal_orchestration_get_contract_summary` | 单合同摘要：状态/风险/链接（Phase 2） |

### 5.2 提案（Plan）

| Tool | 说明 |
|------|------|
| `legal_orchestration_register_files` | 登记对话附件为编排文件项 |
| `legal_orchestration_propose_file_classification` | LLM 语义分类 + 生成 `CLASSIFY_CONFIRM` 提案 |
| `legal_orchestration_propose_type_package` | 缺类型时起草类型包（Phase 2） |
| `legal_orchestration_propose_create_contracts` | 生成 `CREATE_CONTRACTS_BATCH` 提案 |

### 5.3 执行约束

- Agent **禁止**直接调用 `LegalContractService.createContract`。
- 用户在前端点击「确认」→ `POST /legal/orchestration/proposal/execute`。
- 全局编排默认 **禁止静默写操作**（与合同页策略分离）。

---

## 6. 后端接口

### 6.1 编排提案

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/legal/orchestration/proposal/list-pending` | `legal:contract:query` |
| POST | `/legal/orchestration/proposal/execute` | `legal:contract:create` |
| POST | `/legal/orchestration/proposal/cancel` | `legal:contract:create` |

### 6.2 编排会话（Phase 2）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/legal/orchestration/session/get` | 按 conversationId 查询 |

---

## 7. 前端（laby-ui-admin-vben）

### 7.1 组件

- `legal-orchestration-proposal-card.vue`：展示 PENDING 提案，确认/取消
- 挂载于 `ai/chat` 消息列表底部或 assistant 消息区

### 7.2 API

- `#/api/legal/orchestration/index.ts`

### 7.3 交互

1. 用户选择「法务编排」角色，上传合同附件并描述意图。
2. Agent 调用 `propose_*`，返回 `proposalNo`；前端轮询/刷新 pending 列表。
3. 用户点击确认 → execute → 展示创建结果（合同 ID/跳转链接）。

---

## 8. AI 角色配置

- **角色编码**：`legal_orchestration`
- **System Prompt 要点**：
  - 先理解意图，再登记文件
  - 分类必须 `propose_file_classification`，等用户确认
  - 缺类型先说明，Phase 2 再 `propose_type_package`
  - 落库必须 `propose_create_contracts`
  - 禁止未确认写库

---

## 9. 安全与租户

- 所有查询带 `tenant_id`（`TenantBaseDO`）
- 提案 execute 校验 `userId` 与 `conversationId` 归属
- Tool 上下文经 `AiChatHarnessSupport` 注入 `conversationId` + `userId`

---

## 10. 分期交付

| Phase | 内容 | 验收 |
|-------|------|------|
| **P1** | 表/枚举/错误码、编排会话、分类提案、批量创建提案、AI 角色、前端提案卡 | 对话上传→分类确认→创建合同 |
| **P2** | 类型包起草、用户合同查询 Tool、会话 API | 缺类型可起草并确认 |
| **P3** | 跟踪/风险/下载 Tool、SSE 提案推送、菜单权限细化 | 全链路对话闭环 |

---

## 11. 测试要点

1. 未确认时 `legal_contract` 无新增行。
2. 确认后 `create_source=AI_CHAT`，`create_conversation_id` 正确。
3. 提案过期后 execute 返回 `ORCHESTRATION_PROPOSAL_EXPIRED`。
4. 跨租户/跨用户 execute 被拒绝。
