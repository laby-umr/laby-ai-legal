# Wave 2 实施摘要（E3 Playbook + E6 Word 闭环）

> 主 Spec: [2026-06-04-legal-contract-platform-evolution-spec.md](../specs/2026-06-04-legal-contract-platform-evolution-spec.md)

## 已完成

### E3 Playbook
- SQL: `sql/mysql/laby-legal-evol-e3-playbook.sql`
- `LegalReviewPlanCompiler` + `LegalDeterministicAuditEngine`
- API: `/legal/playbook/preview`, `/legal/playbook/simulate`
- AI 审核前自动合并 Playbook 意见（`laby.legal.playbook.enabled=true`）
- 审核规则表单扩展 ruleType / matchPattern

### E4 骨架
- `LegalAiOrchestrator` + `runDeterministicAudit`

### E6 Word 闭环（本次）
- API: `GET /legal/contract/version-diff`
- API: `POST /legal/contract/export-bundle`（交付包 zip）
- 前端：版本对比面板 +「导出交付包」按钮

## SQL 执行顺序

```
laby-legal-evol-e2-clause.sql
laby-legal-evol-e3-playbook.sql
```

## 交付包 zip 结构

```text
{合同标题}-export-第{n}轮.zip
├── manifest.json
├── {标题}-audit-report.docx
├── {标题}-ANNOTATED.docx
├── {标题}-TRACKED.docx   （有采纳意见时）
└── {标题}-CLEAN.docx     （有采纳意见时）
```

## 下一步 Wave 3

- E4 完整：`LegalAiOrchestrator.runAudit` 接管 `doAudit`
- E7：黄金集 eval + CI
- E8：`legal_ai_trace` 观测表
