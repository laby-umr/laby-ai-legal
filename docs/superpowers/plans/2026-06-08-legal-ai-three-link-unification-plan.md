# 法务 AI 三链路统一 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement task-by-task.

**Goal:** 统一编排 / Pipeline / 合同 QA 的 AI 策略（模型、立场、审核强度、审核角色），消除 modelId 断裂与审核标准不一致。

**Architecture:** 新增 `LegalAiPolicyBO` + `LegalAiPolicyResolver` 作为唯一策略层；编排会话持久化 policy 字段；创建合同提案与执行均经 Resolver 校验；Phase B 再抽 AuditKernel。

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus, laby-module-legal + laby-module-ai, Vue3 + Element Plus

**Spec:** `docs/superpowers/specs/2026-06-08-legal-ai-three-link-unification-spec.md`

---

## Phase A — Policy 统一（本次执行）

### Task 1: SQL + 枚举常量

**Files:**
- Create: `sql/mysql/laby-legal-ai-policy-phase-a.sql`
- Create: `laby-module-legal/.../enums/ai/LegalAiPolicyConstants.java`
- Modify: `LegalOrchestrationConstants.java`（默认项委托 PolicyConstants）
- Modify: `ErrorCodeConstants.java`（046/047）

- [ ] 会话表增加 `party_role`, `audit_level`, `audit_role_id`, `policy_json`
- [ ] 常量引用 `LegalPartyRoleEnum` / `LegalAuditLevelEnum`

### Task 2: Policy 领域对象与 Resolver

**Files:**
- Create: `service/ai/policy/bo/LegalAiPolicyBO.java`
- Create: `service/ai/policy/LegalAiPolicyResolver.java`
- Create: `test/.../LegalAiPolicyResolverTest.java`

- [ ] resolveForConversation / resolveForExecute / normalizePartyRole / normalizeAuditLevel
- [ ] modelId 必填，否则抛 ORCHESTRATION_POLICY_MODEL_MISSING

### Task 3: Session 持久化

**Files:**
- Modify: `LegalOrchestrationSessionDO.java`
- Modify: `LegalOrchestrationSessionService.java` + Impl
- Modify: `LegalOrchestrationSessionRespVO.java`

- [ ] getOrCreateSession 同步 policy 字段
- [ ] syncPolicy(sessionId, policy)

### Task 4: 编排链路接入

**Files:**
- Modify: `LegalOrchestrationAiChatHarnessSupport.java`
- Modify: `LegalOrchestrationProposeCreateContractsTool.java`
- Modify: `LegalOrchestrationProposalService.java`
- Modify: `LegalOrchestrationContractCreateExecutor.java`

### Task 5: 前端提案卡

**Files:**
- Modify: `api/legal/orchestration/index.ts`
- Modify: `legal-orchestration-proposal-card.vue`

- [ ] Session/Proposal 展示 modelId、立场、审核强度

### Task 6: 验证

- [ ] `mvn -pl laby-module-legal -am test`（Policy + Session + Executor 单测）
- [ ] `mvn -pl laby-module-legal -am compile`

---

## Phase B — AuditKernel + preview Tool（已完成）

### Task 1: Kernel 抽取
- `LegalAuditKernel` / `LegalAuditKernelImpl`
- `LegalAiAuditServiceImpl` 委托 `runFormal`

### Task 2: 预览解析 + 存储
- `LegalAuditPreviewParseService`
- `LegalOrchestrationPreviewAuditService` → `session.preview_opinion_json`

### Task 3: 编排 Tool
- `legal_orchestration_preview_audit`
- `legal_orchestration_get_audit_preview`

### Task 4: SQL + Prompt
- `sql/mysql/laby-legal-ai-audit-kernel-phase-b.sql`

### Task 5: 验证
- [x] `LegalAuditKernelImplTest`
- [x] `mvn -pl laby-module-legal -am -DskipTests compile`

---

## Phase C — Preview 复用 + 合同页上下文（已完成）

- `LegalAuditPreviewReuseService`：policy 一致时合并 preview + formal（paragraphId+title 去重）
- `LegalAiAuditServiceImpl`：首轮 AI 审核后自动复用
- `legal_ai_trace` 增加 `preview_reuse_count` / `preview_dedupe_count` / `model_fallback`
- 合同审核页展示「来自编排对话」并跳转 `/ai/chat?conversationId=`

SQL：`sql/mysql/laby-legal-ai-preview-reuse-phase-c.sql`

---

## 全链路 SQL 执行顺序

1. `laby-legal-ai-policy-phase-a.sql`
2. `laby-legal-ai-audit-kernel-phase-b.sql`
3. `laby-legal-ai-preview-reuse-phase-c.sql`
