# 法务 AI 三链路统一架构规格

> 版本：1.0  
> 日期：2026-06-08  
> 状态：Phase A/B/C 已实施  
> 前置：`2026-06-05-ai-legal-orchestration-agent-spec.md`  
> 目标：统一模型 / 审核标准 / 审核内核；保留编排、批审、问答三阶段职责分离

---

## 1. 问题陈述

当前三条 AI 链路**入口不同、标准不同、输出不同**，导致用户感知断裂：

| 链路 | 入口 | 模型 | 审核标准 | 输出 |
|------|------|------|----------|------|
| L1 全局编排 | `ai/chat` role=123 | `conversation.modelId` | 编排 System Prompt | 自由对话 + Tool 提案 |
| L2 Pipeline 审核 | 建合同后 BPM | `contract.modelId` | SkillPack + 一审角色 + Playbook | `legal_audit_opinion` JSON |
| L3 合同页 QA | `review.vue` | `contract.modelId` | SkillPack CHAT + QA Agent | 自然语言 + Tool |

**用户期望**：对话选的模型 = 建合同用的模型 = 审核用的模型；对话里看到的风险点与统计页一致。

**结论**：不应合并为一个 Agent，而应统一 **Policy（策略）+ Kernel（审核内核）+ Context（上下文传递）**。

---

## 2. 设计原则

1. **一个标准，三个阶段**：编排 / 批审 / 问答共用 `LegalAiPolicy`，执行方式不同。
2. **正式意见唯一来源**：结构化风险点只来自 `LegalAuditKernel` → `legal_audit_opinion`；编排对话禁止「另起炉灶」长篇审条款。
3. **冻结点明确**：用户确认「创建合同」提案时，冻结 model + partyRole + auditLevel + skillPack 快照。
4. **可审计**：Pipeline 仍走 BPM；预览审核可落 `PREVIEW` 来源标记，正式审核落 `AI`。
5. **最小破坏**：复用 `LegalAiOrchestrator`、`LegalSkillPackSnapshotService`、现有提案机制。

---

## 3. 目标架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    LegalAiPolicyResolver                         │
│  输入：conversation / session / contractType / user overrides   │
│  输出：LegalAiPolicyBO（modelId, partyRole, auditLevel,         │
│        auditRoleId, reauditRoleId, skillPackSnapshotRef）        │
└────────────────────────────┬────────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ L1 Orchestration│ │ L2 AuditKernel  │ │ L3 Contract QA  │
│ Agent (流程)    │ │ (Playbook+LLM)  │ │ Agent (解释)    │
│ Tool: 登记/分类 │ │ 段落批审 JSON   │ │ Tool: 读意见    │
│ Tool: preview   │ │ persist opinion │ │ 禁止 re-audit   │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         └───────────────────┴───────────────────┘
                             ▼
              legal_contract / legal_audit_opinion / legal_agent_proposal
```

### 3.1 三 Agent 职责（统一后）

| Agent | 职责 | 禁止 |
|-------|------|------|
| **Orchestration** | 意图理解、文件登记、分类、类型包、创建提案、进度查询 | 自由发挥完整审核报告替代 Kernel |
| **AuditKernel** | Playbook + 段落批审、写 opinion/report | 直接对用户聊天 |
| **Contract QA** | 基于已落库意见/报告/段落问答 | 调用 LLM 重新全文审核 |

---

## 4. 核心领域对象

### 4.1 `LegalAiPolicyBO`（新建）

统一策略快照，三链路唯一配置源。

```java
public class LegalAiPolicyBO {
    private Long modelId;              // 必填，来自 conversation
    private String partyRole;          // A/B/OTHER
    private String auditLevel;         // standard/strict/...
    private Long auditRoleId;          // 首轮 ai_chat_role.id
    private Long reauditRoleId;        // 二轮，可空
    private String skillPackSnapshotJson; // 创建合同时写入 legal_contract
    private Long conversationId;       // 追溯
    private String policyVersion;      // 如 "2026-06-08-v1"
}
```

**解析优先级**（`LegalAiPolicyResolver`）：

1. 用户提案 payload 显式字段（确认卡勾选）
2. `legal_orchestration_session` 已绑定字段
3. `ai_chat_conversation.modelId` + 对话上下文抽取的 partyRole
4. 合同类型默认 SkillPack + `LegalOrchestrationConstants` 默认项
5. 租户默认 CHAT 模型（仅兜底，应打 warn 日志）

### 4.2 `LegalAuditKernel`（从现有代码抽取）

将 `LegalAiAuditServiceImpl.doAudit` 中 Playbook + LLM 段抽出为可复用服务：

```java
public interface LegalAuditKernel {
    /** 正式审核：需 contractId + 已解析段落 */
    LegalAuditKernelResult runFormal(LegalAuditKernelCommand cmd);

    /** 预览审核：仅需 file + type + policy，不写 contract */
    LegalAuditKernelResult runPreview(LegalAuditPreviewCommand cmd);
}
```

内部仍委托：
- `LegalAiOrchestrator.runPlaybookPhase`
- `LegalAiOrchestrator.runLlmAuditPhase`
- `LegalContractAuditRoleService.resolveSystemMessage`

**预览 vs 正式**：

| 模式 | 段落来源 | 持久化 | sourceType |
|------|----------|--------|------------|
| PREVIEW | 临时 parse infra_file | `legal_orchestration_audit_preview` 或 session JSON | PREVIEW |
| FORMAL | `legal_contract_paragraph` | `legal_audit_opinion` | AI/RULE/... |

正式 Pipeline 创建合同时：若存在同 session 的 PREVIEW 且 policy 一致，可选 **merge/skip-duplicate**（Phase C）。

---

## 5. 数据模型扩展

### 5.1 `legal_orchestration_session` 扩展

| 字段 | 类型 | 说明 |
|------|------|------|
| party_role | VARCHAR(16) | 用户确认的我方立场 |
| audit_level | VARCHAR(32) | 审核强度 |
| audit_role_id | BIGINT | 首轮审核角色 |
| policy_json | TEXT | 完整 LegalAiPolicyBO JSON |
| preview_opinion_json | TEXT | 可选，最近预览结果摘要 |

### 5.2 `legal_agent_proposal.payload` 扩展（CREATE_CONTRACTS_BATCH）

必填/展示字段：

```json
{
  "sessionId": 10,
  "modelId": 7,
  "partyRole": "A",
  "auditLevel": "standard",
  "auditRoleId": 101,
  "skillPackSnapshotHash": "abc123",
  "fileItemIds": [100, 101]
}
```

### 5.3 可选新表 `legal_orchestration_audit_preview`

预览意见与正式 opinion 结构对齐，便于 diff 与迁移。

| 字段 | 说明 |
|------|------|
| session_id | 编排会话 |
| file_item_id | 文件项 |
| policy_hash | 与创建时 policy 比对 |
| opinions_json | List&lt;LegalAiAuditOpinionItemBO&gt; |
| model_id | 使用的模型 |

---

## 6. Tool 与 API 变更

### 6.1 编排 Tool（L1）

| Tool | 变更 |
|------|------|
| `legal_orchestration_propose_create_contracts` | 从 `LegalAiPolicyResolver` 取 policy，payload 写全字段 |
| `legal_orchestration_preview_audit` | **新增**：对已分类 file_item 调 `AuditKernel.runPreview`，返回结构化摘要 |
| `legal_orchestration_get_audit_preview` | **新增**：读 session 预览，供 Agent 回答「有哪些风险」 |

编排 System Prompt 增补：

- 用户问审核时，**必须**先 `preview_audit`，禁止自编风险清单。
- 创建合同前说明：确认后将进入正式 Pipeline，模型与立场以提案卡为准。

### 6.2 提案 API

`POST /legal/orchestration/proposal/execute` 执行 CREATE 时：

1. `LegalAiPolicyResolver.validate(proposal.payload, session, conversation)`
2. `LegalOrchestrationContractCreateExecutor` 写 `legal_contract` 全字段 + skillPackSnapshot
3. `modelId` **禁止** null（否则 1040009000 类业务异常，非 NPE）

### 6.3 合同页 QA（L3）

- `LegalContractAgentPromptHelper` 仅使用 `resolveQaAgentSystemMessage(contract)`（已有 SkillPack 路径）
- Middleware 拦截：若 Tool 名含 `audit` 且非 readonly，拒绝（防 QA Agent 重跑审核）
- 可选：从 `createConversationId` 跳转时展示「与全局对话同一模型/立场」

---

## 7. 前端 UX

### 7.1 编排提案卡 `legal-orchestration-proposal-card.vue`

**创建合同提案**展示并允许编辑（确认前）：

- 模型名称（只读，来自 conversation）
- 我方立场（Select：甲方/乙方/其他）
- 审核强度（Select）
- 合同类型（来自分类结果，只读）
- 「预览审核意见 N 条」链接（只读列表）

### 7.2 统一文案

| 场景 | 文案 |
|------|------|
| 编排对话首次审核 | 「以下为 AI 预览意见，正式意见以创建合同后的审核流程为准」 |
| 创建确认后 | 「已使用对话模型 {modelName} 创建合同，正在启动正式审核…」 |
| 合同页 QA | 「以下回答基于已落库的审核意见，如需重新审核请走 BPM 二轮」 |

---

## 8. 实施分期

### Phase A — Policy 统一（P0，1～2 天）

- [ ] 新建 `LegalAiPolicyBO` + `LegalAiPolicyResolver`
- [ ] `LegalOrchestrationAiChatHarnessSupport` 写入 session.policy_json
- [ ] `LegalOrchestrationContractCreateExecutor` 只用 resolver，消除 NPE
- [ ] 提案 payload 补全 modelId/partyRole/auditLevel
- [ ] 前端提案卡展示 policy 字段

**验收**：conversation.modelId === contract.modelId === 审核 trace 中 modelId

### Phase B — AuditKernel 抽取 + Preview Tool（P1，3～5 天）

- [ ] 抽取 `LegalAuditKernel`，`LegalAiAuditServiceImpl` 改为委托
- [ ] 新增 `preview_audit` Tool + 可选 preview 表
- [ ] 编排 Prompt 强制 preview 路径
- [ ] 单元测试：同一 policy 下 preview 与 formal systemPrompt 一致

**验收**：编排对话展示的风险 title/level 与 preview 表一致；正式审核后 opinion 条数/级别分布合理

### Phase C — 正式审核复用 Preview（P2，可选）

- [ ] 建合同后 Pipeline 读取 preview，dedupe by paragraphId+title
- [ ] 合同页从 conversationId 跳转带 context
- [ ] 监控指标：preview/formal 一致率、模型 fallback 次数

---

## 9. 包结构建议

```
com.laby.module.legal.service.ai
├── policy
│   ├── LegalAiPolicyBO.java
│   ├── LegalAiPolicyResolver.java
│   └── LegalAiPolicyValidator.java
├── kernel
│   ├── LegalAuditKernel.java
│   ├── LegalAuditKernelImpl.java
│   ├── LegalAuditKernelCommand.java
│   └── LegalAuditPreviewCommand.java
└── context
    └── LegalOrchestrationToolRuntimeContext.java  // 已有，增加 policy 引用
```

编排 Tool 仍放 `tool.orchestration`；Kernel 被 `LegalAiAuditServiceImpl` 与 orchestration Tool 共用。

---

## 10. 风险与对策

| 风险 | 对策 |
|------|------|
| Preview 需 parse 未建合同文件 | 复用 `LegalContractPipelineService` 解析段落到临时 BO，或限制仅 PDF/Word |
| Preview 成本高 | 限制每 session 1 次/full；或仅审前 N 段 + 摘要 |
| 旧 session 无 policy_json | Resolver 回退 conversation + 默认值；提案 execute 前强制 refresh |
| QA Agent 越权审核 | Tool 白名单 + Middleware |

---

## 11. 非目标

- 合并三个前端页面为一个 UI
- 编排 Agent 直接写 `legal_audit_opinion`（必须经 Kernel + 提案/ Pipeline）
- 替换 BPM 审核流程
- 统一为一个 ReAct Agent 实例

---

## 12. 评审检查清单

- [ ] 产品：用户是否接受「预览 ≠ 最终」或要求 Phase C merge
- [ ] 法务：partyRole/auditLevel 默认值是否需租户配置
- [ ] 后端：PolicyResolver 单测 + 提案 execute 集成测试
- [ ] 前端：提案卡编辑字段权限与校验

---

## 13. 与现有 spec 关系

本 spec **扩展** `2026-06-05-ai-legal-orchestration-agent-spec.md`：

- 不改变编排状态机主路径
- 新增 Policy/Kernel 层与 preview 能力
- 明确 L2/L3 与 L1 的配置继承关系

实施计划见：`docs/superpowers/plans/2026-06-08-legal-ai-three-link-unification-plan.md`（评审通过后编写）
