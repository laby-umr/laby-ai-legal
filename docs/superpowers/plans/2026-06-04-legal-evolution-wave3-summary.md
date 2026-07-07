# Wave3 实施摘要 — E6 交付闭环 + E7 评测 + E8 追踪 + E4 编排增强

**日期：** 2026-06-04  
**关联规格：** [Laby-Legal-EVOL-001](../specs/2026-06-04-legal-contract-platform-evolution-spec.md)

## 已完成

### E6 Word 交付闭环（上一轮）
- 版本 diff：`GET /legal/contract/version-diff`
- 交付 ZIP：`POST /legal/contract/export-bundle`（报告 + ANNOTATED/TRACKED/CLEAN + manifest）
- 前端：`VersionDiffPanel`、`导出交付包` 按钮

### E4 Orchestrator 增强
- `LegalAiOrchestrator.runPlaybookPhase()` — 编译 Playbook + 确定性检查
- `LegalAiAuditServiceImpl.doAudit` 经编排器执行 Playbook 阶段（LLM 批处理仍在本服务，便于渐进迁移）

### E8 AI 追踪
- SQL：`sql/mysql/laby-legal-evol-e8-trace.sql` → 表 `legal_ai_trace`
- `LegalAiTraceRecorder` — 审核开始/成功/失败写入 trace
- API：`GET /legal/ai-trace/page`（权限 `legal:contract:query`）

### E7 Playbook 评测
- `LegalPlaybookEvalRunner` — 离线黄金集，不依赖 DB/LLM
- 数据集：`src/test/resources/eval/playbook-cases.json`（3 条样例）
- 单测：`LegalPlaybookEvalRunnerTest`

## 本地验证

```bash
mvn -pl laby-module-legal -am test -Dtest=LegalPlaybookEvalRunnerTest,LegalDeterministicAuditEngineTest,LegalContractTextDiffUtilTest,LegalClauseBuilderTest
mvn -pl laby-module-legal -am -DskipTests compile
```

## SQL 追加

```
→ laby-legal-evol-e8-trace.sql
```

## 待办（Wave4 建议）

| Epic | 项 |
|------|-----|
| E4 | 将 `callAuditInBatches` 迁入 `LegalAiAuditPipelineService`，`doAudit` 仅负责状态/BPM |
| E5 | `legal_skill_pack` 表 + 管理端 CRUD |
| E7 | 扩展黄金集 + CI job 门禁（passRate ≥ 阈值） |
| E6 | `laby.legal.audit.unit=CLAUSE` 条款级批审 |
| FE | AI 追踪列表页（可选） |
