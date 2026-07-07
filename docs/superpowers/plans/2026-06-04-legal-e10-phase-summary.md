# E10 Phase 1～3 实施摘要

**日期：** 2026-06-04

## Phase 1（已有 + 收尾）

- SQL：`sql/mysql/laby-legal-evol-e10-onlyoffice.sql`
- OnlyOffice：`LegalDocumentController` / JWT / 短时令牌拉流
- 部署：`docs/deploy/onlyoffice-docker-compose.yml`
- 前端：`ContractOnlyOfficeViewer` + 工作台自动切换 OnlyOffice / docx-preview 降级
- 上传：`.doc/.docx/.pdf`（`create-form.vue`）
- 配置：`application-local.yaml` → `laby.legal.onlyoffice.*`

## Phase 2（本次落地）

### 解析

- `LegalContractPdfParser` — PDF 文字层 → `p-n` 段落
- `LegalFormatConvertService` — DOC→DOCX（需 `laby.legal.format-convert.libre-office-enabled=true`）
- `LegalContractParseServiceImpl` — 按 `source_format` 分支；扫描 PDF / DOC 未转换 → `parse_status=PARTIAL(4)`

### PDF 标准批注（业内对齐）

- `LegalContractPdfAnnotateService` — PDFBox Text 注释
- `POST /legal/contract/export-annotated-pdf`
- 交付包 / 归档包：PDF 合同走 `ANNOTATED.pdf`，Word 仍走 docx POI

### 前端

- `review.vue`：下载合同原件、PDF/docx 标注版自动分流、解析 PARTIAL 提示
- PDF 合同隐藏 Word 采纳版导出

### 依赖

- `laby-module-legal/pom.xml` → `pdfbox 3.0.3`

## Phase 3（本次）

- **OnlyOffice 定位**：`ContractOnlyOfficeViewer` 尝试 `createConnector().SearchNext`（Developer 版）；Community 版自动降级段落视图
- **PDF Highlight**：`LegalContractPdfTextLocator` + 批注导出高亮锚点
- **OCR 预留**：`LegalContractPdfOcrService` + `laby.legal.pdf-ocr.*`（HTTP 端点）
- **编译修复**：`LegalContractPdfParser.splitParagraphs` 改为 public，供 OCR 路径复用

## 验证

```bash
mvn -pl laby-module-legal -am -DskipTests compile
mvn -pl laby-module-legal test "-Dtest=LegalContractPdfParserTest,LegalContractPdfAnnotateServiceTest,LegalPdfTextEvalRunnerTest"
```

（2026-06-04 已通过：6 tests）

## E7 评测扩展（本次）

- `eval/pdf-text-cases.json` — 12 条 PDF 段落用例
- `LegalPdfTextEvalRunner` / `LegalPdfTextEvalGate` — 默认门禁 85%
- `LegalContractPdfAnnotateServiceTest` — Highlight + Text 批注单测

## 启用 OCR（扫描 PDF，可选）

```yaml
laby:
  legal:
    pdf-ocr:
      enabled: true
      endpoint-url: http://your-ocr-service/recognize
```

## 待后续

- OCR 云服务商具体适配
- OnlyOffice Developer 版 license 下定位抽测
- 真实 PDF 样本端到端联调（Acrobat/福昕批注可读性）

## 启用 OnlyOffice（本地）

1. `docker compose -f docs/deploy/onlyoffice-docker-compose.yml up -d`
2. `application-local.yaml`：`laby.legal.onlyoffice.enabled=true`
3. 执行 SQL：`laby-legal-evol-e10-onlyoffice.sql`

## 启用 DOC 解析

```yaml
laby:
  legal:
    format-convert:
      libre-office-enabled: true
      libre-office-path: soffice  # 或 Windows 全路径
```
