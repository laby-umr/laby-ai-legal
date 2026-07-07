# Wave5 实施摘要 — E7 CI 门禁 + 黄金集扩展

**日期：** 2026-06-04

## 已完成

### E7 Playbook 评测 CI 门禁
- 黄金集扩展至 **10 条**（`playbook-cases.json`）
- `LegalPlaybookEvalGate` — 默认 passRate ≥ 100%，可通过 `-Dlegal.eval.minPassRate` 调整
- `LegalPlaybookEvalExpectationBO.maxOpinions` — 支持「不应命中」类断言
- 单测产出 JSON 报告：`target/eval-reports/playbook-eval-report.json`
- GitHub Actions：`.github/workflows/legal-eval.yml`（改 `laby-module-legal` 时触发）

## 验证

```bash
mvn -pl laby-module-legal test "-Dtest=LegalPlaybookEvalRunnerTest,LegalDeterministicAuditEngineTest"
```

## 待办（后续 Wave）

| Epic | 项 |
|------|-----|
| E4 | `doAudit` 进一步瘦身，状态/BPM 与 LLM 管道完全分离 |
| E6 | `laby.legal.audit.unit=CLAUSE` 条款级批审 |
| E7 | 脱敏 docx 样本 + clause_recall / citation 指标 |
| E9 | PDF 文字层 + MCP 边界 |
