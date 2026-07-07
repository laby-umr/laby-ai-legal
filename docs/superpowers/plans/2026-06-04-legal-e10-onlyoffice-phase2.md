# E10 Phase 2 — 多格式解析 + PDF 批注导出 + 定位

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or executing-plans. Depends on [Phase 1](./2026-06-04-legal-e10-onlyoffice-phase1.md) merged.

**Goal:** DOC→docx 转换 + POI 解析；PDF 文字层段落化；**PDF 标准批注导出**（业内三件套）；OnlyOffice 文本 search 定位意见。

**Architecture:** 解析与预览仍解耦；PDF 标注用 PDFBox 写 ISO 32000 Annotation，不改 ORIGINAL；Word 仍 POI。

**Tech Stack:** LibreOffice headless, PDFBox 3.x, Apache POI, OnlyOffice Docs API search

**主 Spec:** [DOC-001 §12.4](../specs/2026-06-04-legal-onlyoffice-document-platform-spec.md)

**预估周期:** 3～4 周

---

## 文件结构（Phase 2 新建/修改）

| 路径 | 职责 |
|------|------|
| `LegalFormatConvertService.java` | DOC→DOCX 异步任务 |
| `LegalContractPdfParser.java` | PDF 文字层 → ParagraphItem |
| `LegalContractPdfAnnotateService.java` | 意见 → ANNOTATED_PDF |
| `LegalContractExportServiceImpl.java` | 按 source_format 分支导出 |
| `script/docker/libreoffice/` | 转换 sidecar（可选） |
| `eval/legal/pdf-text-cases.json` | PDF 段落评测集 |

---

## Task 1: DOC → DOCX 转换管道

**Files:**
- Create: `LegalFormatConvertService.java`, `LegalFormatConvertJob.java`
- Modify: `LegalContractParseServiceImpl.java`

- [ ] **Step 1:** LibreOffice 命令封装

```bash
soffice --headless --convert-to docx --outdir /tmp {input.doc}
```

- [ ] **Step 2:** 异步任务（Spring `@Async` 或 MQ，与现有 parseAsync 一致）
  - 输入：`ORIGINAL` doc 的 fileId
  - 输出：写入 infra + `legal_contract_file`（`role=NORMALIZED_DOCX`, `source_file_id=原 fileId`）
  - 更新 `convert_status`

- [ ] **Step 3:** 转换成功后触发 `LegalContractStructureParser.parse(normalizedBytes)`

- [ ] **Step 4:** 失败：`parse_status=PARTIAL`，预览仍用 OnlyOffice 打开 **ORIGINAL.doc**

- [ ] **Step 5:** 单测：resources 下 1 个小 doc 样本（CI 无 LO 则 `@Disabled` + 文档说明）

---

## Task 2: PDF 文字层解析

**Files:**
- Create: `LegalContractPdfParser.java`
- Modify: `LegalContractParseServiceImpl.java`

- [ ] **Step 1:** PDFBox `PDFTextStripper` 按页提取

- [ ] **Step 2:** 启发式分段（空行、标题正则，与 Word 解析输出同构 `ParagraphItem`）

- [ ] **Step 3:** `detectPdfTextLayer(bytes)`：总字符 / 页数 < 阈值 → `PDF_SCAN`，`parse_status=PARTIAL`

- [ ] **Step 4:** 段落入库 + embedding + audit pipeline（SUCCESS 时）

- [ ] **Step 5:** 单测 + `eval/legal/pdf-text-cases.json`（≥5 样本，段落召回脚本）

---

## Task 3: PDF 标准批注导出（业内对齐）

**Files:**
- Create: `LegalContractPdfAnnotateService.java`
- Modify: `LegalContractExportServiceImpl.java`, `LegalContractController.java`

**Spec 规则（ADR-003）：** 每条意见 → PDF Annotation；能 text match → Highlight；否则 Text 挂页边。

- [ ] **Step 1:** 依赖 `pdfbox` 3.x（检查父 pom 是否已有，无则 `laby-module-legal/pom.xml` 添加）

- [ ] **Step 2:** `annotate(originalPdf, opinions, paragraphIndex)` → `byte[]`
  - `PDAnnotationText` / `PDAnnotationHighlight` + `PDAnnotationPopup`
  - Title = opinion.title
  - Contents = riskLevel + content + suggestion
  - Highlight：PDFTextStripper 定位 `oldText` 或 paragraph.text 的 QuadPoints（找不到则跳过 highlight）

- [ ] **Step 3:** `exportAnnotatedContractPdf(contractId, visibility)` 
  - 写 infra + `legal_contract_file.role=ANNOTATED_PDF`
  - 返回 fileId

- [ ] **Step 4:** Controller `POST /legal/contract/export-annotated-pdf`（权限 `legal:contract:export`）

- [ ] **Step 5:** 归档 ZIP 扩展：`source_format=PDF` 时用 `ANNOTATED.pdf` 替代 `ANNOTATED.docx`

- [ ] **Step 6:** 手工验收：Acrobat Reader / 福昕打开可见批注；ORIGINAL hash 不变

- [ ] **Step 7:** 单测：1 个 2 页 PDF + 2 条意见，断言 Annotation 数量 ≥2

---

## Task 4: Word 导出路径保持不变

**Files:**
- Modify: `LegalContractExportServiceImpl.java`

- [ ] **Step 1:** `exportAnnotatedContractDocx` 仅当 `source_format in (DOCX, DOC)` 且存在 NORMALIZED_DOCX 或 ORIGINAL docx

- [ ] **Step 2:** 前端下载菜单按 `source_format` 显示：
  - docx/doc：标注版 docx + 报告
  - pdf：标注版 pdf + 报告 + 原件

- [ ] **Step 3:** DOC 标注版优先基于 `NORMALIZED_DOCX` 走 POI Comment

---

## Task 5: OnlyOffice 定位（search）

**Files:**
- Modify: `ContractOnlyOfficeViewer.vue`, `review.vue`

- [ ] **Step 1:** 调研 DS Docs API：`asc_docs_api` 的 search 或 `connector` 命令（文档版本 pin 后写死调用方式）

- [ ] **Step 2:** `locateBySearch(text: string)`：意见 `oldText` 优先

- [ ] **Step 3:** `review.vue` 的 `handleLocateParagraph` 在文档视图 call viewer ref

- [ ] **Step 4:** PDF 定位失败 → toast「未找到原文，已打开段落视图」+ 切段落视图

- [ ] **Step 5:** 抽测 20 条 docx 意见定位 ≥90% 命中（人工记录表）

---

## Task 6: parse_status UI 与 AI 门禁

**Files:**
- Modify: `review.vue`, `LegalContractPipelineService.java`

- [ ] **Step 1:** `PARTIAL` / `FAILED` Banner 文案区分 PDF 扫描 vs DOC 转换中

- [ ] **Step 2:** `parse_status != SUCCESS` 时禁用「发起 AI 审核」或二次确认（与产品确认）

- [ ] **Step 3:** 管理端「重新解析」`POST /legal/contract/reparse`（可选）

---

## Task 7: E7 评测扩展

**Files:**
- Create: `eval/legal/pdf-text-cases.json`
- Modify: `LegalPlaybookEvalGate` 或新 runner（可选）

- [ ] **Step 1:** PDF 文字层样本 ≥10，指标：段落召回 ≥85%

- [ ] **Step 2:** CI job 可选 `legal-pdf-parse.yml`（不阻塞主 CI 亦可）

---

## Task 8: 联调验收

- [ ] DOC 样本：转换 + 段落 + AI 审核 + docx 标注导出

- [ ] PDF 样本：段落 + AI 审核 + **ANNOTATED.pdf** + 报告 + 原件 ZIP

- [ ] 三件套 ZIP 结构对照 Spec §12.3

- [ ] 编译 + 测试

```bash
mvn -pl laby-module-legal -am test "-Dtest=LegalContractPdfParserTest,LegalContractPdfAnnotateServiceTest"
```

---

## Phase 2 验收清单（Spec §21.2）

- [ ] DOC 10MB P95 转换 ≤60s（或失败可见）
- [ ] PDF 文字层段落召回 ≥85%
- [ ] docx 意见定位 ≥90%（抽测）
- [ ] PDF search 定位 ≥80%
- [ ] ANNOTATED.pdf 在 Acrobat/福昕可读

---

## Phase 3（可选，另开 Plan）

- 扫描 PDF OCR
- 批注 Flatten 烧录版
- 「对外谈判包」PDF→docx→POI TRACKED
- OnlyOffice edit 模式 + callback 落库
