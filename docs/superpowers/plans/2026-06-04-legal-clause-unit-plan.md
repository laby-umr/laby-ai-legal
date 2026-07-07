# E2 ClauseUnit 结构解析 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development  
> **Spec:** [2026-06-04-legal-contract-platform-evolution-spec.md §8](../specs/2026-06-04-legal-contract-platform-evolution-spec.md)  
> **并行编排:** [Wave1 多窗口](./2026-06-04-legal-evolution-wave1-parallel-orchestration.md) — 本计划对应 **W1 + W2**

**Goal:** 新增 `legal_contract_clause` 与 StructureParser；解析 pipeline 写入 clause 树；**paragraphId 规则不变**。

**Architecture:** `LegalContractStructureParser` 门面协调 `LegalContractWordParser`（段落）+ `LegalClauseBuilder`（树）+ `LegalTableExtractor`（表格 clause）。

**Tech Stack:** Apache POI 5.x, MyBatis, JUnit 5

---

## 并行窗口分配

| 窗口 | Task | 产出 |
|------|------|------|
| **W1** | Task W1 | SQL + DO + Mapper |
| **W2** | Task W2 | Parser + Parse 集成 + 测试 |

---

### Task W1: 数据库与持久层

**Files:**
- Create: `sql/mysql/laby-legal-evol-e2-clause.sql`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/dal/dataobject/clause/LegalContractClauseDO.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/dal/mysql/clause/LegalContractClauseMapper.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/enums/clause/LegalClauseTypeEnum.java`
- Modify: `docs/delivery/README.md` SQL 部署顺序（追加一行）

**SQL 内容 (`laby-legal-evol-e2-clause.sql`):**

- [ ] **Step 1:** `CREATE TABLE legal_contract_clause`（字段见 Spec §16.1）
- [ ] **Step 2:** `ALTER TABLE legal_audit_opinion ADD COLUMN clause_id VARCHAR(32) NULL` + 索引
- [ ] **Step 3:** 菜单占位可选跳过（Workbench 复用 contract 菜单）

- [ ] **Step 4:** `LegalContractClauseDO` 字段与表一致；`paragraphIds` 用 String JSON 存 `@TableField(typeHandler = JacksonTypeHandler.class)` 或逗号分隔 String（与项目现有 JSON 字段风格一致，先 grep 其他 DO）
- [ ] **Step 5:** Mapper 方法：
  - `deleteByContractId(Long contractId)`
  - `selectListByContractId(Long contractId)`
  - `countByContractId(Long contractId)`
  - `selectByContractIdAndClauseId(Long contractId, String clauseId)`

**Verify:**
```bash
./mvnw -pl laby-module-legal -am -DskipTests compile
```

---

### Task W2: 结构解析器

**Files:**
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/bo/LegalClauseUnitBO.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/util/LegalClauseBuilder.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/util/LegalTableExtractor.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/util/LegalContractStructureParser.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/util/LegalContractWordParser.java`（**仅** 如需暴露 paragraph 列表构建，保持 `p-{sort}` 不变）
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalContractParseServiceImpl.java`
- Test: `laby-module-legal/src/test/java/com/laby/module/legal/service/contract/util/LegalClauseBuilderTest.java`
- Test: `laby-module-legal/src/test/resources/legal/sample-contract-headings.docx`（最小 fixture，可程序生成）

- [ ] **Step 1:** `LegalClauseUnitBO` 字段：clauseId, title, level, type, paragraphIds, fullText, path, parentClauseId

- [ ] **Step 2:** `LegalClauseBuilder` V1 规则：
  - POI `XWPFParagraph` 迭代
  - 识别标题：`getStyle()` 含 Heading / outlineLvl / 字号+加粗启发式
  - 识别中文条：`^第[一二三四五六七八九十百零]+条`
  - 识别数字条：`^\d+(\.\d+)*[\s、.]`
  - 非标题段落并入当前 clause 的 paragraphIds + fullText
  - 无标题开头段落 → `c-1` type=CLAUSE

- [ ] **Step 3:** `LegalTableExtractor`：每个 `XWPFTable` → clause type=TABLE，table 前最近 clause 为 parent（或无 parent）

- [ ] **Step 4:** `LegalContractStructureParser.parse(byte[] content)` 返回：
  ```java
  record StructureParseResult(List<ParagraphItem> paragraphs, List<LegalClauseUnitBO> clauses) {}
  ```
  - paragraphs **必须**与单独 `LegalContractWordParser.parseDocx` 结果一致

- [ ] **Step 5:** `LegalContractParseServiceImpl.doParseOnly` 改造：
  ```text
  1. structureParser.parse(content)
  2. 写 paragraph（同现逻辑）
  3. clauseMapper.deleteByContractId + insert clauses
  4. embedContractAsync（不变）
  ```
  - `reparseFromDocxBytes` 同步写 clause

- [ ] **Step 6:** `LegalClauseBuilderTest`：
  - 纯文本 mock 段落列表 → 期望 clause 树
  - 断言 clauseId 连续 c-1,c-2...

- [ ] **Step 7:** 可选：`LegalContractStructureParserIntegrationTest` 用 resources docx

**Run tests:**
```bash
./mvnw -pl laby-module-legal test -Dtest=LegalClauseBuilderTest -DskipTests=false
```

---

### Task W2b: 意见 clauseId 回填（可选 Wave1，可延 INT）

**Files:**
- Modify: `LegalAiAuditServiceImpl.java` 或 opinion 写入处

- [ ] **Step 1:** 写入意见时 `clauseId = clauseMapper.findByParagraphId(contractId, paragraphId)`
- [ ] **Step 2:** 无匹配则 clauseId=null（兼容）

> **注意：** 本任务与 E4 Orchestrator 重叠；Wave1 可只做 Mapper 查询方法，审核写入 INT 再加。

---

### Task W2c: 历史合同回填 Job（P2，Wave1 不强制）

- [ ] `LegalClauseBackfillJob` 按 contractId 列表重跑 parse clause，**不**改 paragraph

---

## 验收（E2 Done）

- [ ] 新上传 docx → clause 表有记录
- [ ] 同 docx 两次解析 paragraphId 完全一致
- [ ] 含「第一条」「第二条」样本 → level/type 合理（人工看 SQL）
- [ ] Parse 失败时 contract 状态仍为 FAILED（现逻辑不变）
- [ ] `./mvnw -pl laby-module-legal -am -DskipTests compile` SUCCESS

---

## 不要做的事

- 不要改 `MAX_PARAGRAPHS_PER_REQUEST`（属 E4）
- 不要用 LLM 解析结构
- 不要在 W1 写 Parser 代码
- 不要删除 `legal_contract_paragraph` 表

---

## paragraphId 稳定性契约（W2 必须单测覆盖）

```java
// 给定固定 docx bytes，两次 parse：
assertEquals(firstRunParagraphIds, secondRunParagraphIds);
// 且与 LegalContractWordParser.parseDocx  alone 结果一致
```
