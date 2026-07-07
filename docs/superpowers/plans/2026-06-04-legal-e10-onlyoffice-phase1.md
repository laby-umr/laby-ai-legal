# E10 Phase 1 — OnlyOffice 预览 + 多格式上传 + 原件下载

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or executing-plans to implement task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 支持 PDF/DOC/DOCX 上传；审阅工作台 **OnlyOffice 高保真只读预览**；**下载原件** 100% 回传上传字节；解析与预览并行（Phase 1 解析仍仅 docx 全功能）。

**Architecture:** 原件 `ORIGINAL` 不可变 → OnlyOffice JWT 拉流预览；`docx-preview` 降级；结构索引沿用现有 POI（docx），DOC/PDF 解析留 Phase 2。

**Tech Stack:** ONLYOFFICE Document Server (Docker), Spring Boot JWT, Vue3 iframe, PDFBox（Phase 2）

**主 Spec:** [Laby-Legal-DOC-001](../specs/2026-06-04-legal-onlyoffice-document-platform-spec.md)

**预估周期:** 2～3 周（1 后端 + 1 前端 + 0.5 运维）

---

## 文件结构（Phase 1 新建/修改）

| 路径 | 职责 |
|------|------|
| `sql/mysql/laby-legal-evol-e10-onlyoffice.sql` | `source_format`、`legal_contract_file.role` 等 |
| `script/docker/onlyoffice/docker-compose.yml` | Document Server 本地/测试部署 |
| `laby-module-legal/.../config/LegalOnlyOfficeProperties.java` | DS URL、JWT secret |
| `laby-module-legal/.../service/document/LegalDocumentPreviewService.java` | 签发 JWT、构建 config |
| `laby-module-legal/.../service/document/LegalOnlyOfficeFileTokenService.java` | 一次性拉流 token |
| `laby-module-legal/.../controller/admin/document/LegalDocumentController.java` | preview-config、file 回调 |
| `laby-module-legal/.../enums/contract/LegalContractSourceFormatEnum.java` | DOCX/DOC/PDF |
| `laby-module-legal/.../enums/contract/LegalContractFileRoleEnum.java` | ORIGINAL 等 |
| `laby-ui/.../workbench/ContractOnlyOfficeViewer.vue` | iframe 嵌入 |
| `laby-ui/.../workbench/ContractWorkbench.vue` | 文档视图切 OnlyOffice |
| `laby-ui/.../modules/create-form.vue` | 三格式上传 |

---

## Task 1: 数据模型与 SQL

**Files:**
- Create: `sql/mysql/laby-legal-evol-e10-onlyoffice.sql`
- Modify: `LegalContractDO.java`, `LegalContractFileDO.java`
- Modify: `DictTypeConstants.java` + 前端 `dict-enum.ts`

- [ ] **Step 1:** 编写 SQL
  - `legal_contract.source_format VARCHAR(16)`（DOCX/DOC/PDF）
  - `legal_contract.parse_detail VARCHAR(255)` nullable
  - `legal_contract_file.role VARCHAR(32) DEFAULT 'ORIGINAL'`
  - `legal_contract_file.format VARCHAR(16)`
  - `legal_contract_file.source_file_id BIGINT` nullable
  - `legal_contract_file.convert_status TINYINT` nullable
  - 字典：`legal_contract_source_format`、`legal_contract_file_role`
  - 数据迁移：`UPDATE legal_contract_file SET role='ORIGINAL', format='DOCX' WHERE ...`（按 file_name 推断）

- [ ] **Step 2:** 更新 DO / Mapper，枚举 `LegalContractSourceFormatEnum`、`LegalContractFileRoleEnum`

- [ ] **Step 3:** 本地执行 SQL，确认无冲突

---

## Task 2: 格式探测与上传校验

**Files:**
- Create: `LegalContractFormatDetector.java`
- Modify: `LegalContractServiceImpl.java`（create + upload）
- Modify: `create-form.vue`

- [ ] **Step 1:** `LegalContractFormatDetector.detect(fileName, bytes)` → DOCX | DOC | PDF
  - 扩展名 + 魔数：`PK\x03\x04`（docx）、`%PDF`、OLE `\xD0\xCF\x11\xE0`（doc）

- [ ] **Step 2:** 创建合同时写入 `source_format`；`legal_contract_file` 写 `role=ORIGINAL`、`format`

- [ ] **Step 3:** 前端 `create-form.vue`
  - `accept=".pdf,.doc,.docx"`
  - 校验与提示文案更新
  - 错误提示三格式 + 大小限制

- [ ] **Step 4:** 单元测试 `LegalContractFormatDetectorTest`（6 个样本字节头）

---

## Task 3: OnlyOffice Document Server 部署

**Files:**
- Create: `script/docker/onlyoffice/docker-compose.yml`
- Create: `script/docker/onlyoffice/README.md`
- Modify: `script/docker/docker-compose.yml`（optional profile 引用）

- [ ] **Step 1:** Compose 服务 `onlyoffice-documentserver`
  - 镜像 `onlyoffice/documentserver`（版本 pin）
  - 端口 8088→80（避免与 laby 冲突）
  - 卷：logs、data、**fonts**（挂载中文字体目录说明）

- [ ] **Step 2:** 启用 DS JWT（`JWT_ENABLED=true`，与 laby 共用 secret）

- [ ] **Step 3:** README：启动命令、healthcheck URL、与 laby-server 网络互通

- [ ] **Step 4:** 本地 `docker compose up -d` 验证 `/healthcheck` 200

---

## Task 4: 应用配置与 JWT 签发

**Files:**
- Create: `LegalOnlyOfficeProperties.java`
- Create: `LegalDocumentPreviewService.java`
- Create: `LegalOnlyOfficeJwtHelper.java`（HMAC SHA256，对齐 DS 文档）

- [ ] **Step 1:** `application.yaml` 增加：

```yaml
laby:
  legal:
    onlyoffice:
      enabled: ${LEGAL_ONLYOFFICE_ENABLED:false}
      document-server-url: ${LEGAL_ONLYOFFICE_URL:http://127.0.0.1:8088}
      jwt-secret: ${ONLYOFFICE_JWT_SECRET:}
      jwt-header: Authorization
      default-mode: view
```

- [ ] **Step 2:** `LegalDocumentPreviewService.buildPreviewConfig(contractId, fileId, userId)`
  - 校验合同归属 + 租户
  - `document.key` = `{tenantId}_{fileId}_{hashPrefix8}`
  - `document.fileType` = docx | doc | pdf
  - `document.url` = `{callbackBase}/admin-api/legal/document/onlyoffice/file/{fileId}?token={oneTime}`
  - `editorConfig.mode` = view
  - `permissions.edit` = false

- [ ] **Step 3:** 单元测试 JWT payload 含 document.url、key 格式

---

## Task 5: 文件拉流与回调 Controller

**Files:**
- Create: `LegalDocumentController.java`
- Create: `LegalOnlyOfficeFileTokenService.java`（Redis 或内存 Cache，token 15min）

- [ ] **Step 1:** `GET /legal/document/preview-config?contractId=`  
  - `@PreAuthorize legal:contract:query`  
  - 返回 `{ documentServerUrl, config }`（config 已含 token 字段）

- [ ] **Step 2:** `GET /legal/document/onlyoffice/file/{fileId}?token=`  
  - **无登录态**（OnlyOffice 服务器调用），仅校验 oneTime token  
  - 流式写回 `Content-Type` + `Content-Disposition`  
  - 大文件用 stream，避免 byte[] 全载入

- [ ] **Step 3:** `POST /legal/document/onlyoffice/callback`  
  - Phase 1：记录 status 日志即可（view 模式无保存）

- [ ] **Step 4:** 集成测试：mock token 拉流 200 + 403 无 token

---

## Task 6: 原件下载 API

**Files:**
- Modify: `LegalContractController.java`, `LegalContractServiceImpl.java`
- Modify: `review.vue` 下载按钮

- [ ] **Step 1:** `GET /legal/contract/download-file?fileId=&role=ORIGINAL`（或扩展现有 download）
  - 默认 `role=ORIGINAL` 当请求「合同原件」

- [ ] **Step 2:** 校验 `legal_contract_file.role` 与 contract 归属

- [ ] **Step 3:** 前端「下载原件」按钮；标注版仍走现有 export（docx 合同）

- [ ] **Step 4:** 手工验证：上传后下载 MD5 与上传一致

---

## Task 7: 前端 OnlyOffice 嵌入

**Files:**
- Create: `ContractOnlyOfficeViewer.vue`
- Create: `api/legal/document/index.ts`
- Modify: `ContractWorkbench.vue`, `review.vue`

- [ ] **Step 1:** API `getDocumentPreviewConfig(contractId)`

- [ ] **Step 2:** `ContractOnlyOfficeViewer.vue`
  - 动态加载 `{documentServerUrl}/web-apps/apps/api/documents/api.js`
  - `new DocsAPI.DocEditor(id, config)`
  - props: `contractId`, `fileId`, `enabled`
  - loading / error / DS 不可用 fallback 文案
  - `expose.locateBySearch(text)` 占位（Phase 2 实现）

- [ ] **Step 3:** `ContractWorkbench.vue`
  - `readerMode === 'docx'` 且 `onlyofficeEnabled` → `ContractOnlyOfficeViewer`
  - 否则 fallback `ContractDocxPreview`（仅 docx）或提示「请用段落视图」

- [ ] **Step 4:** 配置项：从后端 `/legal/document/capabilities` 返回 `{ onlyofficeEnabled, supportedFormats }`（可选）

---

## Task 8: 解析兼容（Phase 1 最小）

**Files:**
- Modify: `LegalContractParseServiceImpl.java`

- [ ] **Step 1:** `source_format=DOCX` → 现有解析不变

- [ ] **Step 2:** `DOC` / `PDF` → Phase 1 设 `parse_status=PARTIAL`，写 `parse_detail=FORMAT_PENDING_PHASE2`，**不阻断**创建与预览

- [ ] **Step 3:** 工作台 Banner：「当前格式结构化解析即将支持，AI 审核可能不可用」

---

## Task 9: 联调与验收

- [ ] **Step 1:** 样本集各 1 份：docx（含页眉页脚）、doc、pdf 文字层

- [ ] **Step 2:** 三格式 OnlyOffice 预览肉眼对比本地 Word/Acrobat

- [ ] **Step 3:** 原件下载 hash 一致

- [ ] **Step 4:** 跨租户 fileId 拉流 403

- [ ] **Step 5:** DS 关闭时 `onlyoffice.enabled=false`，工作台不白屏

- [ ] **Step 6:** 编译

```bash
mvn -pl laby-module-legal -am -DskipTests compile
cd laby-ui/laby-ui-admin-vben && pnpm -F web-ele run typecheck
```

---

## Phase 1 验收清单（对照 Spec §21.1）

- [ ] 可上传 `.doc/.docx/.pdf`
- [ ] `role=ORIGINAL` + `original_hash` 正确
- [ ] OnlyOffice 预览三格式样本
- [ ] 下载原件 MD5 一致
- [ ] DS 降级可用
- [ ] 租户隔离

---

## 下一步

Phase 1 合并后启动 [Phase 2 Plan](./2026-06-04-legal-e10-onlyoffice-phase2.md)：DOC 转换、PDF 解析、PDF 标准批注导出、OnlyOffice search 定位。
