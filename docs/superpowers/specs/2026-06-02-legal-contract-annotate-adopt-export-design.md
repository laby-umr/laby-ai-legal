# 法务合同「标注 / 采纳 / 生成新合同 / 导出」设计（v0.1）

> **日期**：2026-06-02  
> **适用范围**：laby-admin `laby-module-legal` + 前端 `web-ele`  
> **目标**：在 **不破坏原版排版/样式** 的前提下，使用 **Word 原生批注（Comment）**与（可选）**修订（Track Changes）**，实现：
>
> - 首轮 AI 审核后生成「**标注版合同**」（按风险等级呈现）
> - 法务采纳/忽略意见后生成「**采纳版合同**」（同时支持 **带修订** 与 **干净文本** 两种导出）
> - 若走二审：以采纳版为输入，重复标注/采纳并形成新版本链

---

## 1. 术语与约束

### 1.1 术语

- **合同实例（Contract）**：一次审核流程的业务容器（已存在 `legal_contract`）
- **合同版本（ContractVersion）**：一份可下载/可再加工的 docx 文件产物（本文新增）
- **标注（Annotate）**：将意见以 **Word 原生批注 Comment** 写入 docx，并尽量用批注内容的字体颜色体现风险等级
- **采纳（Adopt）**：对被标注合同进行“内容变更”的决策（采纳/忽略），并据此生成下一版本 docx
- **锚点（Anchor）**：用于把意见稳定定位到 docx 中某段/某范围的“隐形标记”

### 1.2 约束（硬约束）

- **必须使用 Word 原生能力**：
  - 标注：Comment
  - 采纳导出：支持 Track Changes（带修订）+ “干净文本”两种导出
- **不得打断原版排版/样式**：
  - 不做正文高亮/下划线/底纹等会改变正文样式的处理（默认策略）
  - 锚点采用 **书签 Bookmark**（不显示、不改排版，但会改变 docx XML）
- **多轮**：
  - 二审输入：以 **采纳后的版本** 作为输入（从用户确认）
  - 轮次上限与 BPM 规则沿用既有设计（见现有 BPM 文档）

---

## 2. 业务流程与版本链（核心设计）

### 2.1 版本链（推荐主路径）

> 每一次“导出可下载 docx”都对应一条 `ContractVersion` 记录。

- **V0 ORIGINAL**：用户上传原文件（仅存储，不改动）
- **V0’ WORKING（内部工作版，可选但推荐）**：在不改变排版前提下插入锚点书签；一般不暴露给用户下载
- **V1 AI_ANNOTATED**：首轮 AI 后生成，写入 Comment（标注版）
- **V2 ADOPTED**：法务采纳/忽略后生成：
  - **V2-TRACKED**：带 Track Changes 的采纳版
  - **V2-CLEAN**：接受修订后的干净文本版（默认仍保留批注；也可导出“去批注”版）
- **二审**：
  - 输入：`V2-CLEAN`（或按实现选择 `V2-TRACKED`，但推荐 CLEAN 以减少噪声）
  - 输出：`V3 RE_AUDIT_ANNOTATED`、`V4 RE_AUDIT_ADOPTED（TRACKED/CLEAN）`

### 2.3 导出分级（内部版 / 外发版）

为避免法务内部意见泄露，导出增加分级：

- **内部版（INTERNAL）**：可保留批注、修订、依据信息
- **外发版（EXTERNAL）**：强制去除内部信息
  - 必须去批注（Comment）
  - 必须接受修订（不保留 Track Changes）
  - 必须去除来源/依据等内部元信息

默认策略：

- 工作台下载默认 INTERNAL
- 对外发送流程默认 EXTERNAL，并在接口侧做强制校验（不允许绕过）

### 2.2 状态机（与 BPM 的结合）

- **AI1 完成**：持久化 opinions + 生成 `V1`
- **法务处置完成**：生成 `V2-TRACKED` 与 `V2-CLEAN`
- **needSecondRound=true**：以 `V2-CLEAN` 作为二审输入，再生成 `V3/V4`
- **归档/导出节点**：以“最新版可用版本”为归档产物，避免只归档报告不归档合同

> 注：报告导出（`exportReportDocx`）与“合同 docx 导出”是两条并行能力；本设计覆盖后者。

---

## 3. 数据模型设计（最小新增集合）

### 3.1 新增表：`legal_contract_version`

用途：记录每一份可下载/可再加工的 docx 文件版本。

关键字段：

- `id`
- `contract_id`
- `audit_round`：1/2（与 opinions/report 对齐）
- `version_no`：0/1/2/…
- `type`（枚举）：
  - `ORIGINAL`
  - `WORKING`（内部）
  - `AI_ANNOTATED`
  - `ADOPTED_TRACKED`
  - `ADOPTED_CLEAN`
  - `RE_AUDIT_ANNOTATED`
  - `RE_AUDIT_ADOPTED_TRACKED`
  - `RE_AUDIT_ADOPTED_CLEAN`
- `source_version_id`：派生来源（形成链）
- `file_id`：关联 `infra_file` / `legal_contract_file`
- `anchor_snapshot_id`：本版本使用的锚点快照
- `create_time` / `creator`
- `visibility`（枚举）：`INTERNAL` / `EXTERNAL`
- `immutable_hash`：该版本文件 hash（用于审计与防篡改校验）

### 3.2 新增表：`legal_anchor_snapshot`、`legal_anchor_item`

用途：为每个可再加工版本提供稳定定位能力（“意见 → Word 范围”）。

- `legal_anchor_snapshot`
  - `id`
  - `contract_id`
  - `version_id`
  - `content_hash`：整文 hash（可选）
  - `create_time`
- `legal_anchor_item`
  - `id`
  - `snapshot_id`
  - `anchor_id`：建议直接复用 `legal_contract_paragraph.id`
  - `bookmark_name`：如 `laby_p_<paragraphId>`
  - `paragraph_hash`：段落文本 hash（用于冲突检测/漂移检测）
  - `paragraph_index` / `path`：可选 fallback，用于极端情况下重定位

### 3.3 增强：`legal_audit_opinion`（写回必需字段）

为实现“采纳→写回→生成新合同”，需要 opinion 具备可执行的变更描述。

新增/明确字段（建议）：

- `from_version_id`：该意见基于哪一版合同生成（通常是 `V0’` 或 `V2-CLEAN`）
- `anchor_id`：定位到段落（先段落级）
- `change_type`（枚举）：
  - `REPLACE` / `INSERT_BEFORE` / `INSERT_AFTER` / `DELETE` / `NO_CHANGE`
- `old_text`：可选但强烈建议；用于写回校验，避免错改
- `new_text`：建议修改后的文本（REPLACE/INSERT 必填）

补充：为支持「规则与条款」驱动的自动写回，建议增加 opinion 的来源与引用字段：

- `source_type`：`AI` / `RULE` / `STANDARD_CLAUSE` / `MANUAL`
- `source_id`：规则 ID / 标准条款 ID /（或 AI 批次 ID）
- `source_version`：规则/条款版本号（或发布时间戳）
- `reference_text`：引用的条款原文片段（可选，便于导出批注展示“依据”）
- `source_snapshot_text`：生成意见时的依据快照文本（可截断或存 hash+快照表）
- `priority`：用于同段落多条写回排序（若不单独字段，则可由 risk level + source_type 推导）

---

## 4. 锚点设计（不破坏排版的稳定定位）

### 4.1 锚点载体：Word 书签（Bookmark）

为每个段落插入一对书签标签：

- `w:bookmarkStart` / `w:bookmarkEnd`
- 书签名：`laby_p_<paragraphId>`（保证唯一）

**为什么选 Bookmark：**

- 不显示，不影响排版
- Word 原生，适合挂 Comment 与定位修订范围
- 可在后续生成中复用，定位稳定性优于“文本搜索”

### 4.2 WORKING 版（V0’）生成规则

输入：`V0 ORIGINAL`  
输出：`V0’ WORKING` + `anchor_snapshot`

步骤：

- 解析 docx，建立“段落 → paragraphId”映射（与 `legal_contract_paragraph` 对齐）
- 对每个段落写入 Bookmark（段首/段尾）
- 落库 `legal_anchor_snapshot` 与 `legal_anchor_item`

> V0’ 可以对外隐藏，只作为后续 V1/V2 的生成基底；对外仍认为“不改变原版排版”。

### 4.3 原件证据链（V0 不可变）

为满足法务举证与复盘，V0 需建立不可变存证：

- 上传即计算并持久化 `original_hash`（推荐 SHA-256）
- 记录上传人、上传时间、原文件名、MIME
- V0 仅允许读取，不允许覆盖更新
- 后续任意版本均通过 `source_version_id` 链接回 V0，可追溯完整演进路径

---

## 5. V1 标注版（AI_ANNOTATED）：Comment 写入策略

### 5.1 Comment 内容模板（风险等级颜色体现在批注内容）

批注内容建议结构：

- 第一行：等级标签（字体颜色区分）
  - `【HIGH】` 红色
  - `【MEDIUM】` 橙色
  - `【LOW】` 蓝/灰
- 后续：标题 / 风险描述 / 建议改法 / 引用依据（纯文本）

> 说明：Word 批注气泡/侧边颜色属于 UI 层，不保证可控；采用“批注内容字体色”是可实现且稳定的方式。

补充：当 opinion 来自规则/标准条款时，批注中应显示引用信息（不影响排版）：

- `来源：规则 <ruleName>` 或 `来源：标准条款 <clauseTitle>`
- `依据：<reference_text>`（可截断）

### 5.2 Comment 绑定范围

- 以 `anchor_id` 定位到书签范围
- 将 Comment 挂在该范围（段落级）

### 5.3 产物

- 生成 `legal_contract_version(type=AI_ANNOTATED)`，写入 `legal_contract_file`
- opinions 继续以卡片形式展示；Word 仅作为导出/流转载体

---

## 6. V2 采纳版（ADOPTED）：Track Changes 与“干净版”双导出

### 6.1 带修订版（ADOPTED_TRACKED）

输入：`V1`（或 `V0’`） + opinions（含 adopt/ignore）  
输出：`V2-TRACKED`

规则：

- 仅对 `ADOPTED` 的意见执行写回：
  - `REPLACE`：替换书签范围文本
  - `INSERT_BEFORE/AFTER`：在范围前/后插入新文本
  - `DELETE`：删除范围文本
- 开启 Track Changes，使改动以修订痕迹呈现
- 批注保留，并在批注末尾追加状态：
  - `状态：已采纳 / 已忽略`

生成闸门（法务确认）：

- 在执行自动写回前，提供“生成前预览清单”（采纳条目、变更类型、冲突项）
- 对高风险条款或来源为 RULE/STANDARD_CLAUSE 的自动改写，可配置“必须人工确认后才能生成”

### 6.2 干净版（ADOPTED_CLEAN）

输入：`V2-TRACKED`  
输出：`V2-CLEAN`

规则：

- “接受全部修订”生成干净正文
- 默认仍保留批注（便于审计与沟通）；可加导出参数支持“去批注”

外发规则覆盖：

- 当 `visibility=EXTERNAL` 时，强制去批注并输出 CLEAN，不允许导出带修订或内部依据信息

---

## 7. 冲突与安全策略（必须落地）

### 7.1 `old_text` 校验（防错改）

若 opinion 提供 `old_text`：

- 写回前对比当前书签范围文本
- 不一致则判为 **冲突**，该条不自动写回，进入“需要人工处理”列表（前端提示）

### 7.2 同段多条采纳（重叠修改）

默认执行顺序：

1. 风险等级：HIGH → MEDIUM → LOW
2. 来源优先级（建议）：`MANUAL`（人工明确修改）→ `STANDARD_CLAUSE` → `RULE` → `AI`
3. 创建时间：早 → 晚

一旦在某条写回中触发 `old_text` 不匹配或范围不存在：

- 停止后续同段写回（避免级联错改）
- 标记冲突并提示用户

### 7.3 人工收尾后的再生成

若用户在“人工收尾”阶段手改了合同文本并形成新的输入版本（建议显式生成一个 `FINALIZED` 或复用 `ADOPTED_CLEAN` 另起 version_no）：

- 后续二审与导出以“最新可编辑版本”为输入
- 需要重新生成 WORKING 版与 anchor snapshot（避免锚点漂移）

---

## 8. 二审链路（基于采纳版再审）

### 8.1 二审输入选择

- 默认：以 `V2-CLEAN` 作为二审输入（减少修订噪声）
- 同时附带二审上下文摘要（非正文）：
  - 未采纳意见列表及原因
  - 冲突未自动写回列表
  - 一审高风险残留项

### 8.2 二审产物

- `V3 RE_AUDIT_ANNOTATED`：写入 Comment（同 V1）
- `V4 RE_AUDIT_ADOPTED_TRACKED / CLEAN`：同 V2

opinions 必须绑定 `audit_round=2` 且 `from_version_id=V2-CLEAN`，保证可追溯。

---

## 9. 接口草案（后端）

> 命名仅供参考；最终应对齐现有 `LegalContractController` 风格。

### 9.1 生成/导出标注版合同（V1）

- `POST /legal/contract/export-annotated-docx`
  - req：`contractId`（默认 latest round）、`visibility=INTERNAL|EXTERNAL`
  - resp：`fileId` 或下载 URL
  - 行为：若 V1 已存在可直接返回；否则基于 V0’ 生成

### 9.2 生成/导出采纳版合同（V2）

- `POST /legal/contract/export-adopted-docx`
  - req：`contractId`、`mode=TRACKED|CLEAN`、`removeComments=false|true`、`visibility=INTERNAL|EXTERNAL`
  - resp：`fileId`
  - 行为：必要时先生成 TRACKED 再生成 CLEAN；写入 `legal_contract_version`
  - 约束：`visibility=EXTERNAL` 时强制 `mode=CLEAN` 且 `removeComments=true`

### 9.3 生成前预检（建议新增）

- `POST /legal/contract/precheck-adopted-export`
  - req：`contractId`、`auditRound`
  - resp：可自动写回条数、冲突条数、需人工确认条数、风险分布
  - 行为：供前端“生成前确认”弹窗使用

### 9.4 二审导出

同上，按 `auditRound` 与 `latestVersion` 选择输入版本。

---

## 10. 前端交互建议（web-ele）

审核工作台 `review.vue`：

- AI 首轮完成后展示：
  - 「导出标注版（批注）」→ 调 9.1
- 采纳/忽略完成后展示：
  - 「导出采纳版（带修订）」→ 9.2(mode=TRACKED)
  - 「导出采纳版（干净文本）」→ 9.2(mode=CLEAN)
- 若进入二审：按钮文案替换为“二审标注版/二审采纳版”

冲突提示：

- 当导出采纳版存在冲突条目时，导出 API 返回结构化冲突列表，前端提示用户去处理对应意见（或跳转段落）

法务确认弹窗（建议）：

- 调用 9.3 预检接口，展示：
  - 将自动写回的条目数
  - 冲突条目数
  - 需要人工确认的条目（高风险/规则来源）
- 用户确认后才触发最终导出

---

## 11. 非功能与验收

### 11.1 不破坏排版

- V1/V2 输出 docx 的正文排版、段落样式、编号结构应与输入版本一致（除非 Track Changes 引入的可视修订标记）
- 书签插入不应改变可视布局

### 11.2 可追溯

- 任何一份导出的 docx 都可追溯到：
  - 输入 version、anchor snapshot、opinions（round）
- opinions 必须带 `from_version_id`

### 11.3 最小验收用例

- 一份带编号与表格的合同：
  - V1 批注能正确挂到对应段落
  - V2-TRACKED 能对采纳意见产生修订痕迹
  - V2-CLEAN 能生成无修订痕迹的正文，且排版不乱

---

## 12. 上线策略（法务风险视角）

结论：**以 B（结构化可写回）为目标，A（提示不改文）为兜底**，分阶段上线。

### 12.1 分层启用策略

- **P0（首发）**：仅对“高置信、模板化、低歧义”的规则/条款启用自动写回（B）
- **P0 同步兜底**：其余规则走 A（只生成批注与建议，不自动改正文）
- **P1（稳定后）**：根据误改率与人工驳回率逐步扩大 B 覆盖范围

### 12.2 风险闸门（自动写回必过）

- 必须通过 `old_text` 校验；不匹配则禁止自动写回
- 同段重叠/冲突意见时，停止后续级联改写并提示人工处理
- 默认先给法务“带修订版”复核，再按需导出“干净版”

### 12.3 运营指标（用于是否扩大 B）

- 自动写回成功率（无冲突且可落文）
- 人工回退/驳回率（已采纳后仍手工反改）
- 导出后复核通过率（法务一次通过占比）

当“误改可控、复核通过率稳定”时，再扩大规则自动写回范围。

---

## 13. 总架构重排（知识库 / 向量库 / 问答 / 导出一体化）

> 本节用于修正“仅围绕导出设计”的偏差：导出是产物层，质量核心在知识治理与检索层。

### 13.1 六层架构（主路径）

1. **数据源层**
   - 合同原文（上传 docx）
   - 标准条款库
   - 审核规则库
   - 法律法规/案例库
   - 历史审核记录（可回流）
2. **知识治理层**
   - 入库清洗、切片、标签化（合同类型/风险域/业务线）
   - 版本管理（draft/published/deprecated）
   - 生效域管理（scope）
3. **检索层（RAG）**
   - 向量召回（语义 TopK）
   - 结构检索（关键词/条款号/规则命中）
   - 混合重排（语义分 + 规则优先级 + 时效）
4. **推理层（双引擎）**
   - 审核引擎：产出 opinion（含 evidence 与 change）
   - 问答引擎：基于同一 evidence 回答“依据/原因/后果”
5. **决策层**
   - 采纳/忽略/人工改写
   - 预检闸门（冲突、高风险确认）
   - 二审触发（仅针对残留高风险/冲突）
6. **产物层**
   - 审核报告（Markdown/Word）
   - 合同版本 V1/V2/V3/V4（标注/采纳）
   - INTERNAL / EXTERNAL 分级导出

### 13.2 统一证据链（审核与问答共用）

- 审核 opinion 与 QA answer 必须共享 evidence 引用机制
- 每次召回必须持久化（可追溯“当时为什么这么判、怎么答”）
- 对外展示时可隐藏内部细节，但内部审计必须可还原

### 13.3 新增核心对象（建议）

- `legal_knowledge_item`
  - `type`：RULE / STANDARD_CLAUSE / LAW / CASE
  - `version`、`status`、`effective_from/to`、`scope`
- `legal_knowledge_chunk`
  - `item_id`、`chunk_index`、`chunk_text`、`embedding_id`
- `legal_retrieval_log`
  - `biz_type`（AUDIT / QA）
  - `biz_id`（opinionId / qaMessageId）
  - `query`、`topk`、`retrieved_chunk_ids`、`rerank_score`
- `legal_qa_session` / `legal_qa_message`
  - 问答会话与消息
  - `evidence_refs`（引用规则/条款/合同段落）

### 13.4 审核链路（RAG 驱动）

1. 合同解析分段并建立段落锚点
2. 合同段落 embedding 入索引（合同侧）
3. 每段审核时执行“知识侧 + 合同侧”混合召回
4. LLM 输出结构化 opinion：
   - 风险等级
   - 建议修改（change_type / old_text / new_text）
   - evidence_refs（必须）
5. 法务处置后生成 V1/V2 文档版本
6. 若触发二审，输入 `V2-CLEAN + 残留问题摘要`

### 13.5 问答链路（复用同一检索与证据）

- 问答不得绕开审核检索链路，避免“审核一套、问答一套”导致口径不一致
- 每条回答应包含：
  - 结论
  - 依据来源（规则版本/条款编号/合同段落）
  - 风险后果（若不修改）
  - 可选替代写法

---

## 14. 按模块拆分的实施任务单（可排期）

### 14.1 后端任务（Java / legal 模块）

1. **数据模型与 DDL**
   - 新增：`legal_contract_version`、`legal_anchor_snapshot`、`legal_anchor_item`
   - 新增：`legal_knowledge_item`、`legal_knowledge_chunk`、`legal_retrieval_log`
   - 新增：`legal_qa_session`、`legal_qa_message`
   - 扩展：`legal_audit_opinion` 来源/证据/改写字段
2. **知识治理服务**
   - 知识项 CRUD + 发布流程（draft -> published）
   - 版本与生效域校验
3. **向量化与检索服务**
   - chunk embedding 任务
   - 混合召回 + 重排
   - retrieval log 持久化
4. **审核引擎重构**
   - 审核改为 RAG 驱动输出 opinion（强制 evidence_refs）
5. **问答引擎**
   - 基于同一 retrieval 服务
   - 回答结构化输出与证据引用
6. **文档生成引擎**
   - V0’ 锚点生成
   - V1 Comment 写入
   - V2 TrackChanges/Clean 生成
7. **导出与安全控制**
   - INTERNAL / EXTERNAL 强约束
   - 预检接口（冲突/人工确认）
8. **BPM 集成**
   - AI1 后生成 V1
   - 处置后生成 V2
   - 二审输入 `V2-CLEAN + 摘要`

### 14.2 前端任务（web-ele）

1. **审核工作台增强**
   - 展示 evidence 来源（规则/条款/段落）
   - 导出按钮：标注版、采纳版（带修订/干净）
2. **生成前预检弹窗**
   - 自动写回条数、冲突条数、需人工确认条数
3. **问答面板**
   - 对合同问答
   - 支持从 opinion 卡片一键追问“为什么这样改”
4. **导出分级 UI**
   - INTERNAL / EXTERNAL 选择（并显示外发限制说明）

### 14.3 测试任务（必须覆盖）

1. **文档稳定性**
   - 编号、目录、表格、页眉页脚在 V1/V2 后不乱
2. **写回准确性**
   - `old_text` 匹配/不匹配分支
   - 同段冲突中断策略
3. **证据可追溯**
   - opinion 与 QA 均能回放 retrieval log
4. **安全导出**
   - EXTERNAL 无批注、无修订、无内部依据
5. **二审一致性**
   - `V2-CLEAN` 输入 + 残留摘要生效

### 14.4 建议排期（两周）

- **D1-D3**：DDL + DO/Mapper + 枚举 + 基础 API
- **D4-D7**：检索服务 + 审核引擎（RAG）+ retrieval log
- **D8-D10**：文档引擎（V0’/V1/V2）+ 导出分级
- **D11-D12**：问答面板 + 预检弹窗 + 全链路联调
- **D13-D14**：回归测试 + 灰度发布


