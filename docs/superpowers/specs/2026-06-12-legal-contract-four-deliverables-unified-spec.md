# 法务合同「四件套交付」统一 Spec（业内对齐 · 可执行）

| 属性 | 值 |
|------|-----|
| **文档编号** | Laby-Legal-DELIV-001 |
| **版本** | v1.0 |
| **日期** | 2026-06-12 |
| **状态** | **Approved for Implementation** |
| **模块** | `laby-module-legal` · `laby-ui/web-ele` · OnlyOffice Document Server |
| **战略叙事** | **意见在 DB · 活文档在 WORKING · 用户只见四件套 · 下载按需生成** |
| **上游文档** | [DOC-001 OnlyOffice 平台](./2026-06-04-legal-onlyoffice-document-platform-spec.md) · [标注/采纳设计 v0.1](./2026-06-02-legal-contract-annotate-adopt-export-design.md) · [EVOL-001](./2026-06-04-legal-contract-platform-evolution-spec.md) |
| **取代/收敛** | 收敛 DOC-001 §12 中「多 version 快照缓存」做法；与 v0.1 设计 §2.1 版本链 **产品语义对齐、实现策略升级** |

---

## 目录

1. [执行摘要](#1-执行摘要)
2. [背景与根因](#2-背景与根因)
3. [业内对标](#3-业内对标)
4. [目标与非目标](#4-目标与非目标)
5. [术语](#5-术语)
6. [架构原则（ADR 摘要）](#6-架构原则adr-摘要)
7. [产品模型：用户四件套](#7-产品模型用户四件套)
8. [内部模型：仅 ORIGINAL + WORKING](#8-内部模型仅-original--working)
9. [意见处置 → 文档同步规则](#9-意见处置--文档同步规则)
10. [四件套生成算法（Word）](#10-四件套生成算法word)
11. [四件套生成算法（PDF）](#11-四件套生成算法pdf)
12. [OnlyOffice 同步协议](#12-onlyoffice-同步协议)
13. [段落锚点与定位](#13-段落锚点与定位)
14. [数据模型与版本表策略](#14-数据模型与版本表策略)
15. [API 契约](#15-api-契约)
16. [前端契约](#16-前端契约)
17. [归档与交付包](#17-归档与交付包)
18. [分阶段实施（可执行）](#18-分阶段实施可执行)
19. [验收标准](#19-验收标准)
20. [风险与回滚](#20-风险与回滚)
21. [附录 A：现状代码映射](#21-附录-a现状代码映射)
22. [附录 B：manifest.json  schema](#22-附录-b-manifestjson--schema)

---

## 1. 执行摘要

法务合同审阅对外 **只提供 4 类下载**，与 Ironclad / CompareX / 国内法务台一致：

| # | 用户名称 | 英文代号 | 含义 |
|---|----------|----------|------|
| 1 | **源文件** | `ORIGINAL` | 上传原件，字节与 hash 不变 |
| 2 | **标注版** | `ANNOTATED` | 挂审阅意见，**不改正文** |
| 3 | **修订版** | `REVISION` | **已采纳**且可写回的意见，以 **修订痕迹** 呈现 |
| 4 | **采纳版** | `ADOPTED` | **当前工作稿干净正文**（与编辑页一致） |

**实现策略变更（相对现状）：**

- 内部持久化 **仅** `ORIGINAL` + `WORKING`（活文档）；不再把 `AI_ANNOTATED` / `ADOPTED_TRACKED` / `ADOPTED_CLEAN` 当作「下载缓存」。
- 四件套 **下载时按需生成**（或用户点「归档/完成审阅」时打 **immutable 发布包** 一次）。
- 采纳/撤销/批量处置后 **全量重建 WORKING**（禁止单条 incremental patch 导致与修订版不一致）。
- PDF 不提供 Word 式修订/采纳正文改写；走 **标注 PDF + 可选 Word 谈判版** 分支（与 DOC-001 ADR-003 一致）。

**预估工期：** P0（Word 四件套一致）2～3 周；P1（PDF + 锚点）1～2 周。

---

## 2. 背景与根因

### 2.1 用户诉求

- 创建合同后：定位、采纳、下载应与 **编辑页（OnlyOffice）** 一致。
- 只关心 4 个文件，不理解系统内多种 version type。
- 修订版应与「已采纳几条意见」一致；标注版应反映 **处置结果**（非忽略意见）。

### 2.2 现状缺陷（2026-06-12 代码审计）

| 问题 | 现象 | 根因 |
|------|------|------|
| 版本过多 | UI 见 ORIGINAL / WORKING / AI_ANNOTATED / ADOPTED_* 多轮多条 | 导出快照落库 + `ensure*` 只写一次 |
| 修订≠采纳 | 修订版与编辑页 WORKING 不一致 | 修订走 `readExportSourceBytes` 全量重算；采纳走 WORKING 增量 patch |
| 标注含忽略意见 | 标注版意见数偏多 | `exportAnnotated` 未过滤 `IGNORED` |
| 缓存不刷新 | 新采纳后下载仍是旧文件 | `existsVersion(...)` 短路 |
| PDF 全禁用 | 四按钮不可用 | 前端 `disabled: isPdf` |
| 锚点漂移 | 批注/改写落空 | `p-n` 序号与 POI/OnlyOffice 段落不一致 |

---

## 3. 业内对标

| 能力 | Ironclad / CLM 常见 | Word 审阅 | 本 Spec |
|------|---------------------|-----------|---------|
| 原件 | Executed / Source | 原件 | `ORIGINAL` |
| 标注 | Comment layer | 批注 | `ANNOTATED` |
| 修订 | Redline / Blackline | 修订 | `REVISION`（Track Changes） |
| 采纳 | Clean copy | 接受修订后 | `ADOPTED`（WORKING 干净稿） |
| PDF | Annotated PDF + 原件 | 无 Track Changes | 标注 PDF；修订/采纳→谈判 docx |
| 版本策略 | 发布包 immutable | 单活文档 | WORKING 活 + 按需/归档固化 |

---

## 4. 目标与非目标

### 4.1 目标

| ID | 目标 |
|----|------|
| G1 | 用户下载区 **固定 4 项**，文案与业内一致 |
| G2 | **采纳版 ≡ 编辑页 WORKING**（forceSave 后） |
| G3 | **修订版 = 已采纳可写回意见** 的 Track Changes，与 DB 中 ADOPTED 集合一致 |
| G4 | **标注版 = 非 IGNORED 意见** 的 Comment/Annotation，不改正文 |
| G5 | 源文件 hash 永不变 |
| G6 | DOC / DOCX / PDF 格式策略矩阵清晰可测 |

### 4.2 非目标

| ID | 非目标 |
|----|--------|
| NG1 | PDF 上实现 Word 级 Track Changes / 正文采纳写回 |
| NG2 | 在线协同多用户实时改合同（OnlyOffice co-edit） |
| NG3 | 每次下载都永久新增 version 行（仅归档时固化） |
| NG4 | 外发版自动邮件发送（仍由现有 BPM/导出负责） |

---

## 5. 术语

| 术语 | 定义 |
|------|------|
| **意见真源** | `legal_audit_opinion` 表，含 `status`：PENDING / ADOPTED / IGNORED |
| **ORIGINAL** | 上传字节 + SHA-256，只读 |
| **WORKING** | 当前轮次唯一可编辑 docx（OnlyOffice 绑定）；PDF 合同时为 OnlyOffice 保存的 PDF 或原件视图 |
| **按需生成** | 下载 API 实时渲染，不写 `legal_contract_version`（除 manifest 可选） |
| **发布包** | 用户「完成审阅/归档」时生成的 immutable ZIP，内含四件套 + manifest |
| **可写回意见** | `LegalAuditOpinionRewriteSupport.isAdoptApplicableToDocument(opinion)==true` |

---

## 6. 架构原则（ADR 摘要）

| ADR | 决策 |
|-----|------|
| **DELIV-001** | 用户只见四件套；内部仅 ORIGINAL + WORKING |
| **DELIV-002** | 四件套默认 **按需生成**；`legal_contract_version` 仅记录 ORIGINAL、WORKING、**PUBLISHED_BUNDLE** |
| **DELIV-003** | 意见变更后 **全量** `rebuildWorkingFromAdoptedOpinions`，禁止单条 incremental patch |
| **DELIV-004** | 修订版底稿 = forceSave 后的 WORKING（去 Comment）；采纳版 = WORKING 去修订痕 |
| **DELIV-005** | 标注版底稿 = ORIGINAL（Word）或 ORIGINAL（PDF）；意见过滤 `status != IGNORED` |
| **DELIV-006** | PDF：REVISION/ADOPTED 不对 PDF 正文做 POI 改写；提供 **Word 谈判版** 可选 |

---

## 7. 产品模型：用户四件套

### 7.1 下载区 UI（唯一入口）

`ContractWorkbenchDownloads.vue` / `review.vue` 下载 Tab **仅 4 行**：

| key | 标签 | 副标题 |
|-----|------|--------|
| `original` | 源文件 | 上传原件，未修改 |
| `annotated` | 标注版 | 审阅意见（批注），正文不变 |
| `revision` | 修订版 | 已采纳意见之修订痕迹 |
| `adopted` | 采纳版 | 与当前编辑页一致的干净正文 |

**禁止** 向用户展示：`AI_ANNOTATED`、`ADOPTED_TRACKED`、`ADOPTED_CLEAN`、`WORKING` 等内部枚举名。

### 7.2 意见集合定义

| 交付物 | 意见集合 |
|--------|----------|
| 标注版 | `audit_round = R` 且 `status IN (PENDING, ADOPTED)` |
| 修订版 | `status = ADOPTED` 且 `isAdoptApplicableToDocument = true` |
| 采纳版 | 不直接枚举意见；以 **WORKING 文档状态** 为准（已含全部 ADOPTED 写回 + 人工编辑） |
| 源文件 | 无 |

### 7.3 用户可见说明（PDF）

> 本合同为 PDF：支持源文件、标注版（标准 PDF 批注）。修订/采纳请使用「Word 谈判版」或在审阅报告中查看已采纳意见。

---

## 8. 内部模型：仅 ORIGINAL + WORKING

### 8.1 持久化文件角色

| role | 必须 | 说明 |
|------|------|------|
| `ORIGINAL` | ✅ | 上传时写入，`main_flag=true` |
| `WORKING` | ✅ Word/DOC/DOCX | 带 bookmark 的工作 docx；OnlyOffice 编辑目标 |
| `WORKING` | 可选 PDF | OnlyOffice 另存 PDF 时使用；默认预览 ORIGINAL |
| `PUBLISHED_BUNDLE` | 归档时 | ZIP + manifest |

**废弃作为下载缓存：** `AI_ANNOTATED`、`ADOPTED_TRACKED`、`ADOPTED_CLEAN` 的 **自动 ensure\*** 写入（见 §14）。

### 8.2 版本表 `legal_contract_version.type` 收敛

| type | 保留 | 用途 |
|------|------|------|
| `ORIGINAL` | ✅ | 审计 |
| `WORKING` | ✅ | 编辑 revision 追踪 |
| `PUBLISHED` | ✅ 新增 | 归档 immutable 包（可选） |
| `AI_ANNOTATED` | ⚠️ 只读历史 | 迁移期兼容；新流程不再写入 |
| `ADOPTED_*` | ⚠️ 只读历史 | 同上 |

---

## 9. 意见处置 → 文档同步规则

### 9.1 触发点

| 事件 | 后端动作 |
|------|----------|
| `adopt` / `batchAdopt` | 更新 status → **全量** `rebuildWorkingFromAdoptedOpinions(contractId)` |
| `revoke`（原 ADOPTED 且可写回） | status → PENDING → **全量 rebuild** |
| `ignore` | 仅 DB；**不**改 WORKING 正文（标注版下载时自然排除） |
| `applyRiskAnnotations`（可选） | 将 PENDING+ADOPTED 意见写入 WORKING Comment（Word）；**不**代替四件套导出 |
| OnlyOffice `forceSave` | 更新 WORKING 字节 + `documentRevision` |

### 9.2 全量重建算法（Word）

```
输入: ORIGINAL 或 round>1 时上一轮 PUBLISHED ADOPTED 底稿
步骤:
  1. base = insertParagraphBookmarks(输入)
  2. adopted = 当前轮全部 status=ADOPTED 且 isAdoptApplicableToDocument
  3. WORKING = adopted 为空 ? base : renderAdoptedClean(base, adopted)
  4. 写 infra + 更新 WORKING version 行（versionNo++，immutableHash）
```

**禁止：** `applyAdoptedOpinionsToWorking(contractId, List.of(singleOpinion))` 增量 patch。

### 9.3 与编辑页一致性

- 用户在 OnlyOffice **手工改字** → 仅存在于 WORKING。
- **采纳版下载** = forceSave 后的 WORKING（可选 `stripComments` + 接受修订）。
- **修订版下载** = 以 WORKING（去 Comment）为底，对 **当前 ADOPTED 可写回集合** 执行 `renderAdoptedTracked`；若 WORKING 已含采纳正文，则 tracked 仅对「尚未体现在正文中的 ADOPTED」补修订痕，或 **简化策略**：revision 一律从 `bookmarkBase + renderAdoptedTracked(all ADOPTED)` 生成（与 WORKING 正文允许略有差异时以 manifest 声明）。

**P0 简化策略（推荐）：**

- **采纳版** = WORKING（forceSave，strip comments/track）
- **修订版** = `insertParagraphBookmarks(ORIGINAL)` + `renderAdoptedTracked(全部 ADOPTED 可写回)`  
  → 保证与 DB 采纳集合 100% 一致；与 WORKING 差异由 manifest `adoptedCount` 说明。

---

## 10. 四件套生成算法（Word）

### 10.1 前置：同步

所有非 `original` 下载前：

```
POST /legal/contract/document/sync-working?contractId=
  → OnlyOffice forceSave → 更新 WORKING
```

### 10.2 源文件 `ORIGINAL`

```
bytes = read(ORIGINAL.fileId)
Content-Type = 按扩展名
filename = {title}-源文件.{ext}
```

### 10.3 标注版 `ANNOTATED`

```
base = read(ORIGINAL)  // 不改 WORKING，避免带上采纳改写
opinions = round R, status in (PENDING, ADOPTED)
out = renderAnnotated(base, opinions)
format = docx
filename = {title}-标注版-第{R}轮.docx
```

### 10.4 修订版 `REVISION`

```
syncWorking()
base = insertParagraphBookmarks(read(ORIGINAL))
adopted = round R, ADOPTED, adoptApplicable
out = renderAdoptedTracked(base, adopted)
format = docx
filename = {title}-修订版-第{R}轮.docx
```

### 10.5 采纳版 `ADOPTED`

```
syncWorking()
out = stripComments(stripTrackChanges(read(WORKING)))
format = docx
filename = {title}-采纳版-第{R}轮.docx
```

### 10.6 DOC 上传

- 解析/编辑前：`DOC → NORMALIZED_DOCX`（LibreOffice）。
- 四件套 **输出均为 docx**；源文件下载仍为 **ORIGINAL.doc**。

---

## 11. 四件套生成算法（PDF）

| 交付物 | PDF 行为 |
|--------|----------|
| 源文件 | ORIGINAL.pdf |
| 标注版 | `LegalContractPdfAnnotateService.annotate(ORIGINAL, opinions)` → .pdf |
| 修订版 | **不生成 PDF**；UI 显示「Word 谈判版」按钮 → `PDF→docx` + §10.4 |
| 采纳版 | **不生成 PDF**；同上 → §10.5；或隐藏并指向谈判版 |

**谈判版生成（P1）：**

```
docx = LibreOffice.convert(ORIGINAL.pdf)
然后走 §10.4 或 §10.5
```

---

## 12. OnlyOffice 同步协议

| 步骤 | 责任 |
|------|------|
| 打开工作台 | `ensureWorkingVersion`；JWT 指向 WORKING fileId |
| 采纳/撤销后 | 后端 rebuild WORKING → `documentRevision` 变更 → 前端 reload iframe |
| 下载前 | `forceSaveAndWait(timeout=8s)` |
| PDF view | mode=view；edit 依 license；save 回 WORKING PDF（可选） |

---

## 13. 段落锚点与定位

### 13.1 锚点策略

| 优先级 | 机制 |
|--------|------|
| 1 | Bookmark `laby_p_{paragraphId}`（WORKING 创建时写入） |
| 2 | OnlyOffice `SearchNext` + `oldText` / 段落全文 |
| 3 | 段落侧栏 scroll（降级） |

### 13.2 解析对齐

- 入库 `legal_contract_paragraph.paragraphId` 与 `insertParagraphBookmarks` 使用 **同一编号规则**。
- 导出 `render*` 优先 **bookmark 定位**，`p-n` 序号仅作 fallback。

---

## 14. 数据模型与版本表策略

### 14.1 不删除列；改变写入策略

- `legal_contract_version` 保留历史 `AI_ANNOTATED` / `ADOPTED_*` 行。
- 新代码路径：**下载 API 不再调用** `ensureAnnotatedVersion` / `ensureAdoptedVersions`。
- 新增可选表 `legal_contract_publish_log`（P1）：

| 字段 | 说明 |
|------|------|
| contract_id | |
| audit_round | |
| bundle_file_id | ZIP |
| manifest_json | §22 |
| adopted_count / annotated_count | |
| working_hash | |

### 14.2 `legal_contract_file.role` 收敛

| role | 用途 |
|------|------|
| ORIGINAL | 源文件 |
| WORKING | 内部 |
| ANNOTATED_PDF | PDF 标注缓存（可选，仍按需生成优先） |
| PUBLISHED_BUNDLE | 归档 ZIP |

---

## 15. API 契约

### 15.1 统一下载（推荐 P0）

```
GET /admin-api/legal/contract/download-deliverable
  ?contractId=
  &deliverable=ORIGINAL|ANNOTATED|REVISION|ADOPTED
  &auditRound=   // 默认当前轮
```

响应：文件流或 `{ fileId }` 重定向现有 `downloadContractFile`。

### 15.2 同步

```
POST /admin-api/legal/contract/document/sync-working?contractId=
→ { revision: string, workingFileId: long }
```

### 15.3 废弃/兼容

| 旧 API | 新行为 |
|--------|--------|
| `export-annotated-docx` | 内部转 `deliverable=ANNOTATED` |
| `export-adopted-docx?mode=TRACKED` | `deliverable=REVISION` |
| `export-adopted-docx?mode=CLEAN` | 废弃；由 `deliverable=ADOPTED` 取代 |
| `export-annotated-pdf` | `deliverable=ANNOTATED` + PDF 分支 |

### 15.4 权限

- `legal:contract:query` — 四件套下载
- `legal:contract:update` — sync-working（编辑场景）

---

## 16. 前端契约

### 16.1 `ContractWorkbenchDownloads.vue`

- 4 行固定；PDF 时 revision/adopted 换「Word 谈判版」或 disabled+tooltip（见 §7.3）。
- 每次点击下载：`syncWorking` → `downloadDeliverable` → blob 下载。
- **不**再 `pickVersion('AI_ANNOTATED')` 读缓存。

### 16.2 `review.vue`

- 采纳成功提示：「已更新工作稿，采纳版与编辑页一致；修订版反映全部已采纳意见。」
- 版本对比 Tab：改为 **WORKING vs ORIGINAL** 或 **REVISION vs ORIGINAL**（可选 P1）。

### 16.3 API 封装

```ts
export type ContractDeliverable = 'ORIGINAL' | 'ANNOTATED' | 'REVISION' | 'ADOPTED';

export function syncWorkingDocument(contractId: number) { ... }
export function downloadDeliverable(contractId: number, deliverable: ContractDeliverable, auditRound?: number) { ... }
```

---

## 17. 归档与交付包

完成审阅（`completeOpinionReview`）或 BPM 归档节点：

```
ZIP/
  01-源文件.{ext}
  02-标注版.{docx|pdf}
  03-修订版.docx          // Word only
  04-采纳版.docx          // Word only
  05-审核报告.docx
  manifest.json
```

manifest 见 §22。写入 `PUBLISHED_BUNDLE` **一次**，immutable。

---

## 18. 分阶段实施（可执行）

### Phase P0 — Word 四件套一致（必须）

| # | 任务 | 文件 |
|---|------|------|
| P0-1 | adopt/revoke/batch → 全量 `rebuildWorkingFromAdoptedOpinions` | `LegalAuditOpinionServiceImpl` |
| P0-2 | 删除/绕过 `ensureAnnotatedVersion`、`ensureAdoptedVersions` 在下载路径的调用 | `LegalContractExportServiceImpl` |
| P0-3 | 实现 `LegalContractDeliverableService`（§10 算法） | 新建 |
| P0-4 | `download-deliverable` + `sync-working` Controller | `LegalContractController` |
| P0-5 | 标注过滤 IGNORED | `DeliverableService` |
| P0-6 | 前端 4 按钮 + 新 API | `ContractWorkbenchDownloads.vue`, `api/legal/contract` |
| P0-7 | 单测：3 条 ADOPTED → revision 含 3 处修订；adopted = WORKING | `LegalContractDeliverableServiceTest` |

### Phase P1 — PDF + 锚点

| # | 任务 |
|---|------|
| P1-1 | PDF `ANNOTATED` → `export-annotated-pdf` 接入 deliverable |
| P1-2 | PDF 谈判版：`PDF→docx` + Word revision/adopted |
| P1-3 | Bookmark 优先定位；eval 段落对齐 |

### Phase P2 — 归档与审计

| # | 任务 |
|---|------|
| P2-1 | `completeOpinionReview` 自动生成 PUBLISHED_BUNDLE |
| P2-2 | `legal_contract_publish_log` |
| P2-3 | 外发版：revision/adopted strip 内部批注 |

---

## 19. 验收标准

### 19.1 功能

| ID | 场景 | 期望 |
|----|------|------|
| AC-1 | 上传 docx，AI 10 条意见，忽略 2、采纳 3 | 标注版 8 条批注；修订版 3 处修订痕；采纳版与 WORKING 一致 |
| AC-2 | 采纳后再撤销 1 条 | rebuild 后修订版 2 处；WORKING 同步 |
| AC-3 | OnlyOffice 手工改一句后下载采纳版 | 含手工修改 |
| AC-4 | 源文件 hash 与上传一致 | 任意轮次不变 |
| AC-5 | PDF 合同 | 源文件 + 标注 PDF 可下；谈判 docx 可选 |
| AC-6 | 完成审阅归档 | ZIP 含 manifest + 四件套 + 报告 |

### 19.2 非功能

| ID | 指标 |
|----|------|
| NF-1 | 按需生成标注/修订 < 5s（50 段、30 意见） |
| NF-2 | sync-working 超时 8s 可配置 |

### 19.3 回归

- 现有 BPM 流程、`completeOpinionReview`、二轮审核不受影响。
- 历史 `AI_ANNOTATED` 版本只读可下，新流程不再新增该类行。

---

## 20. 风险与回滚

| 风险 | 缓解 |
|------|------|
| 按需生成慢 | 归档时预生成 bundle；大文件异步 |
| WORKING 与 REVISION 策略差异 | P0 双轨：adopted=WORKING，revision=ORIGINAL+tracked；manifest 说明 |
| OnlyOffice save 失败 | 下载前 toast 警告；禁止 silent 降级 |
| 回滚 | Feature flag `laby.legal.deliverable-v2.enabled=false` 恢复旧 export API |

---

## 21. 附录 A：现状代码映射

| Spec 概念 | 现状类/文件 | 改造 |
|-----------|-------------|------|
| ORIGINAL | `LegalContractVersionServiceImpl.ensureOriginalVersion` | 保留 |
| WORKING | `ensureWorkingVersion`, `applyAdopted*` | rebuild 全量化 |
| ANNOTATED | `LegalContractDocxRenderUtil.renderAnnotated` | 经 DeliverableService 调用 |
| REVISION | `renderAdoptedTracked` | 同上 |
| ADOPTED | WORKING bytes | forceSave + strip |
| PDF 标注 | `LegalContractPdfAnnotateService` | deliverable 分支 |
| 下载 UI | `ContractWorkbenchDownloads.vue` | 4 按钮 |
| 缓存问题 | `ensureAnnotatedVersion` L265 | 下载路径移除 |

---

## 22. 附录 B：manifest.json schema

```json
{
  "contractId": 1001,
  "title": "采购合同",
  "auditRound": 1,
  "sourceFormat": "DOCX",
  "generatedAt": "2026-06-12T10:00:00+08:00",
  "originalSha256": "...",
  "workingSha256": "...",
  "opinionStats": {
    "total": 12,
    "pending": 4,
    "adopted": 5,
    "ignored": 3,
    "adoptApplicable": 4
  },
  "files": {
    "original": "01-源文件.docx",
    "annotated": "02-标注版.docx",
    "revision": "03-修订版.docx",
    "adopted": "04-采纳版.docx",
    "report": "05-审核报告.docx"
  },
  "deliverableSpec": "Laby-Legal-DELIV-001-v1.0"
}
```

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-06-12 | 初版：四件套业内对齐 + 可执行 Phase/API/验收 |
