# 六章合同审核报告 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `buildReportMarkdown` 升级为与 PRD 样例 **六章结构 1:1 对齐** 的 Markdown 报告，并用于 AI 审核落库与 `rebuildAuditReportIfMissing`。

**Architecture:** 新建 `LegalAuditReportBuilder`，从 `LegalContractDO` + 意见列表（`AiOpinionItem` 或 DO）程序化组装六章；第一、四章摘要由合同元数据与意见聚合生成（V1 不额外调 LLM）；第二、三、五章直接映射意见；第六章由高/中风险意见生成行动项。

**Tech Stack:** Java 17、Hutool、现有 `legal_audit_report.content` Markdown 字段、前端 MarkdownView。

**上游 PRD:** [2026-06-01-legal-contract-review-prd-1to1.md](../requirements/2026-06-01-legal-contract-review-prd-1to1.md) 第四节

---

### Task 1: LegalAuditReportBuilder

**Files:**
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalAuditReportBuilder.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalAiAuditServiceImpl.java`
- Test: `laby-module-legal/src/test/java/com/laby/module/legal/service/contract/LegalAuditReportBuilderTest.java`

- [x] **Step 1:** 实现六章 Markdown 构建（页眉 + 一至六章 + 表格）
- [x] **Step 2:** `LegalAiAuditServiceImpl` 与 `rebuildAuditReportIfMissing` 改用 Builder
- [ ] **Step 3:** 单元测试断言章节标题与表格列存在
- [ ] **Step 4:** `mvn -pl laby-module-legal test -Dtest=LegalAuditReportBuilderTest`

---

### Task 2: 后续迭代（不在本次范围）

- [ ] 报告 Word 导出与 Markdown 结构对齐
- [ ] 第一章/第四章 LLM 摘要增强（单独 summary 调用）
- [ ] 前端报告目录锚点 / 折叠
