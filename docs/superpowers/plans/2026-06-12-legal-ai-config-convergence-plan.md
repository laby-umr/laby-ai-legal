# 法务 AI 配置收敛（CFG-001 P0）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落实 CFG-001 P0：审核规则校验、法务工具 API 对齐、前端配置引导，减少六处菜单双配。

**Architecture:** 后端在既有 Service 层加校验与新只读 API；前端改表单 help 与动态工具列表；不改动 Policy/Kernel 主链路。

**Tech Stack:** Spring Boot · MyBatis · Vue3 · Vben Admin · JUnit5

**Spec:** [2026-06-12-legal-ai-config-convergence-spec.md](../specs/2026-06-12-legal-ai-config-convergence-spec.md)

---

### Task 1: 审核规则 PREFERRED_CLAUSE 校验

**Files:**
- Modify: `laby-module-legal/.../enums/ErrorCodeConstants.java`
- Modify: `laby-module-legal/.../service/auditrule/LegalAuditRuleServiceImpl.java`
- Create: `laby-module-legal/src/test/java/.../LegalAuditRuleServiceImplTest.java`

- [ ] 新增错误码 `AUDIT_RULE_PREFERRED_REQUIRES_STANDARD_CLAUSE`
- [ ] `createAuditRule` / `updateAuditRule` 调用 `validateRuleBusinessRules`
- [ ] 单测：PREFERRED 无 standardClauseId 抛异常；CUSTOM_LLM 可空

### Task 2: 法务 Agent 工具列表 API

**Files:**
- Create: `LegalSkillPackLegalToolRespVO.java`
- Modify: `LegalSkillPackService.java` · `LegalSkillPackServiceImpl.java`
- Modify: `LegalSkillPackController.java`

- [ ] `GET /legal/skill-pack/legal-agent-tools`
- [ ] 合并 `AiToolService.getToolListByStatus` 与 `LegalSkillPackToolNames.ALLOWED`

### Task 3: 前端 API + 技能包表单

**Files:**
- Modify: `laby-ui/.../api/legal/skill-pack/index.ts`
- Modify: `laby-ui/.../views/legal/skill-pack/data.ts`

- [ ] `getLegalAgentTools()` 
- [ ] `toolNames` 动态 options + fallback

### Task 4: 审核规则 / 合同类型 / 新建合同表单引导

**Files:**
- Modify: `audit-rule/data.ts`
- Modify: `contract-type/data.ts`
- Modify: `contract/create/data.ts`

- [ ] PREFERRED 强制 standardClauseId
- [ ] help 文案 per spec §7

### Task 5: 验证

- [ ] `mvn -pl laby-module-legal test "-Dtest=LegalAuditRuleServiceImplTest"`
- [ ] 编译 legal 模块通过

---
