# Wave4 实施摘要 — E5 SkillPack + E4 LLM 管道

**日期：** 2026-06-04

## 已完成

### E5 SkillPack
- SQL：`sql/mysql/laby-legal-evol-e5-skillpack.sql`
- 表 `legal_skill_pack` + `legal_contract_type` 绑定字段
- CRUD API：`/legal/skill-pack/*`（含 `copy`、`simple-list`）
- `LegalSkillPackRegistry`：按合同类型解析 AUDIT/CHAT 包、Tool 白名单过滤
- **AUDIT**：合同类型 SkillPack → `chat_role_id` Prompt + `model_policy.maxTokens`
- **CHAT**：Agent 问答同样走 CHAT SkillPack（Prompt + Tool 子集 + maxTokens）
- 前端：`views/legal/skill-pack/*`，合同类型表单可选技能包

### E4 Orchestrator + Pipeline
- `LegalAiAuditPipelineService`：LLM 分批审核从 `LegalAiAuditServiceImpl` 抽离
- `LegalAiOrchestrator.runLlmAuditPhase()`：`doAudit` 经编排器调用管道
- `LegalAiAuditOpinionItemBO`：统一意见 DTO

### 菜单与观测
- SQL：`sql/mysql/laby-legal-evol-menu.sql`（AI 技能包 + AI 审核追踪菜单）
- 前端：`views/legal/ai-trace/index`（只读 trace 列表）

### E5 SkillPack 快照 + CHAT Trace（Wave4 续）
- SQL：`sql/mysql/laby-legal-evol-e5-contract-snapshot.sql`
- 合同创建时写入 `legal_contract.skill_pack_snapshot`（AUDIT/CHAT 版本快照）
- `LegalSkillPackSnapshotService`：快照构建 + 运行时优先读快照，无快照回退合同类型
- AUDIT/CHAT Prompt、Tool、maxTokens 均走快照优先解析
- CHAT 场景写入 `legal_ai_trace`（`scene=CHAT`），同步/流式均覆盖

## SQL 顺序（追加）

```
→ laby-legal-evol-e5-skillpack.sql
→ laby-legal-evol-menu.sql
→ laby-legal-evol-e5-contract-snapshot.sql
```

## 验证

```bash
mvn -pl laby-module-legal -am -DskipTests compile
mvn -pl laby-module-legal test "-Dtest=LegalPlaybookEvalRunnerTest"
```
