# E1 法务审阅工作台 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development  
> **Spec:** [2026-06-04-legal-contract-platform-evolution-spec.md §7](../specs/2026-06-04-legal-contract-platform-evolution-spec.md)  
> **并行编排:** [Wave1 多窗口](./2026-06-04-legal-evolution-wave1-parallel-orchestration.md) — 本计划对应 **W3 + W4**

**Goal:** 交付三栏法务工作台：目录 | 原文 | 意见联动；聚合 API `get-workbench`。

**Architecture:** 新增 `LegalContractWorkbenchService` 聚合现有 get/list 能力；前端拆 `workbench/*` 组件，`review.vue` 瘦身为壳。

**Tech Stack:** Java 17, Spring Boot, Vue3, Element Plus, Vben

---

## 并行窗口分配

| 窗口 | Task | 产出 |
|------|------|------|
| **W3** | Task W3 | 后端 API + VO + Service |
| **W4** | Task W4 + W4b | 前端三栏 + review 重构 |

---

### Task W3: Workbench 聚合 API

**Files:**
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/controller/admin/contract/vo/LegalContractWorkbenchRespVO.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/controller/admin/contract/vo/LegalContractWorkbenchNavigationNodeVO.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/controller/admin/contract/vo/LegalContractWorkbenchReportSummaryVO.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalContractWorkbenchService.java`
- Create: `laby-module-legal/src/main/java/com/laby/module/legal/service/contract/LegalContractWorkbenchServiceImpl.java`
- Modify: `laby-module-legal/src/main/java/com/laby/module/legal/controller/admin/contract/LegalContractController.java`（**仅追加** endpoint）
- Test: `laby-module-legal/src/test/java/com/laby/module/legal/service/contract/LegalContractWorkbenchServiceImplTest.java`

- [ ] **Step 1:** 定义 `LegalContractWorkbenchRespVO`（字段对齐编排文档 §3.1 契约）
- [ ] **Step 2:** `LegalContractWorkbenchServiceImpl.getWorkbench(contractId)`：
  - 调 `contractService.getContractResp`
  - 调 `paragraphMapper.selectListByContractId`
  - 调 `opinionMapper` 当前轮次意见列表（复用现有 list 逻辑）
  - 报告摘要：查 `legal_audit_report` 最新一条，解析 riskHighCount（或从 contract/opinion 统计）
- [ ] **Step 3:** `buildParagraphNavigation(paragraphs)` → mode=PARAGRAPH 的 nodes
- [ ] **Step 4:** 预留 `buildClauseNavigation(contractId)`：
  ```java
  // TODO wave1-int: if clauseMapper.countByContractId > 0 then CLAUSE else PARAGRAPH
  ```
- [ ] **Step 5:** Controller 追加：
  ```java
  @GetMapping("/get-workbench")
  @PreAuthorize("@ss.hasPermission('legal:contract:query')")
  ```
- [ ] **Step 6:** 单测：空段落、有意见、无报告三场景

**Run test:**
```bash
cd d:/IdeaProject/laby-admin
./mvnw -pl laby-module-legal test -Dtest=LegalContractWorkbenchServiceImplTest -DskipTests=false
```

**Run compile:**
```bash
./mvnw -pl laby-module-legal,laby-server -am -DskipTests compile
```

---

### Task W4: 前端 API 与 composable

**Files:**
- Modify: `laby-ui/laby-ui-admin-vben/apps/web-ele/src/api/legal/contract/index.ts`
- Create: `laby-ui/laby-ui-admin-vben/apps/web-ele/src/views/legal/contract/workbench/types.ts`
- Create: `laby-ui/laby-ui-admin-vben/apps/web-ele/src/views/legal/contract/workbench/useWorkbenchSync.ts`

- [ ] **Step 1:** `index.ts` 增加类型 `LegalContractWorkbenchResp` 与 `getWorkbench(contractId)`
- [ ] **Step 2:** `useWorkbenchSync.ts`：
  - `activeParagraphId` ref
  - `highlightParagraph(id)` scroll + CSS class
  - `selectOpinion(opinion)` → 读 `opinion.paragraphId` 定位
  - `selectParagraph(id)` → 右栏 scroll 到关联意见
- [ ] **Step 3:** 若 W3 未合并，临时 fallback：
  ```typescript
  async function loadWorkbench(contractId: number) {
    try {
      return await getWorkbench(contractId);
    } catch {
      const [contract, paragraphs, opinions] = await Promise.all([...]);
      return assembleParagraphMode(...);
    }
  }
  ```

---

### Task W4b: 三栏组件

**Files:**
- Create: `laby-ui/.../workbench/ClauseTree.vue`（PARAGRAPH 模式下显示段落列表）
- Create: `laby-ui/.../workbench/ContractReader.vue`
- Create: `laby-ui/.../workbench/OpinionPanel.vue`
- Modify: `laby-ui/.../review.vue`

- [ ] **Step 1:** `ClauseTree.vue`
  - props: `nodes`, `mode`, `activeId`
  - emit: `select(id, paragraphIds)`
  - ElTree 或自定义列表；level>0 缩进
- [ ] **Step 2:** `ContractReader.vue`
  - props: `paragraphs`, `activeParagraphId`, `highlightOpinionIds?`
  - 每段 `data-paragraph-id`；active 段左边框 + 背景色（风险色可选 P2）
  - `watch activeParagraphId` → `scrollIntoView`
- [ ] **Step 3:** `OpinionPanel.vue`
  - 复用/包装现有 `opinion-card.vue` 逻辑
  - 筛选：风险等级 ElSelect、来源 sourceType、状态
  - emit: `select`, `adopt`, `ignore`（事件上浮 review.vue 现有 handler）
- [ ] **Step 4:** `review.vue` 重构：
  - 保留顶栏：标题、状态、BPM、导出、Agent 抽屉
  - **替换** 原 Tabs 中「段落+意见」为三栏 `flex` 布局（1080p min-height）
  - 报告 Tab 保留或并入右栏子 Tab
  - `onMounted` 调 `loadWorkbench`
- [ ] **Step 5:** `readonlyMode` 下隐藏处置按钮，保留联动
- [ ] **Step 6:** 样式：`.legal-workbench { display:flex; height: calc(100vh - 220px); }`

---

### Task W4c: 集成自测清单

- [ ] 打开 `/legal/contract/review?id={id}` 三栏渲染
- [ ] 点击意见 → 中间栏滚动到段落
- [ ] 点击左栏段落 → 右栏相关意见可见
- [ ] AI 审核中 progress 面板仍显示（顶栏或右栏上方）
- [ ] BPM 嵌入 props.id 路径正常

---

### Task INT-W3: CLAUSE 分支（主会话，依赖 W2）

**Files:**
- Modify: `LegalContractWorkbenchServiceImpl.java`
- Modify: `LegalContractWorkbenchServiceImplTest.java`

- [ ] **Step 1:** 注入 `LegalContractClauseMapper`
- [ ] **Step 2:** `buildNavigation` 有 clause 时构建树（parent_clause_id）
- [ ] **Step 3:** 单测追加 CLAUSE 场景

---

## 验收（E1 Done）

- [ ] UX-E1-01～08（见 Spec §7.6）手工通过
- [ ] get-workbench P95 <800ms（500 段以内，本地粗测）
- [ ] 无 regression：意见 adopt/ignore API 仍走原 handler

---

## 不要做的事

- 不要在本 Epic 改 `LegalAiAuditServiceImpl`
- 不要删除旧 `getParagraphList` API（Workbench 内部可复用）
- 不要一次性重写 1100 行 review.vue 所有 Tab — 仅替换审阅主区
