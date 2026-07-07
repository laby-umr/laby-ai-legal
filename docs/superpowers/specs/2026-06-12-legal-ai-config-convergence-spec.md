# 法务 AI 配置收敛 Spec（CFG-001）

| 属性 | 值 |
|------|-----|
| **文档编号** | Laby-Legal-CFG-001 |
| **版本** | v1.0 |
| **日期** | 2026-06-12 |
| **状态** | **Approved for Implementation** |
| **模块** | `laby-module-legal` · `laby-module-ai` · `laby-ui/web-ele` |
| **上游文档** | [EVOL-001](./2026-06-04-legal-contract-platform-evolution-spec.md) · [三链路统一](./2026-06-08-legal-ai-three-link-unification-spec.md) |
| **问题来源** | 合同类型 / 标准条款 / 审核规则 / AI 技能包 / 聊天角色 / 工具管理 六处配置入口重叠，运营易双配、运行时优先级不直观 |

---

## 1. 执行摘要

六块能力 **不是重复实现**，而是 **业务知识层**（类型、条款、规则）与 **AI 编排层**（技能包、角色、工具）叠在一起。本 Spec 目标：

1. **明确单一职责边界**（文档 + UI 引导）
2. **消除双通道配置**（法务工具、提示词优先级）
3. **表单校验防双写**（规则 vs 条款正文）
4. **不破坏现有运行时**（兼容历史合同与 Policy 快照）

**预估工期：** P0 校验与引导 3～5 天；P1 配置中枢 UI 1 周；P2 解析链可观测 3 天。

---

## 2. 现状与重叠点

| 对比 | 重复度 | 根因 |
|------|--------|------|
| 标准条款 ↔ 审核规则 | 中（内容） | `ruleContent` 可粘贴条款全文，未强制 `standardClauseId` |
| 技能包 ↔ 聊天角色 | 中（入口） | 技能包引用角色；合同还有 `auditRoleId`；三处可配提示词 |
| 技能包 ↔ 工具管理 | 高 | 法务 Agent 读 `toolNames` 白名单；通用 AI 读 `ai_tool` + `tool_ids` |
| 合同类型 ↔ 知识库 | 中 | 法务 RAG 用 `knowledgeId`；聊天角色另有 `knowledgeIds` |
| 合同类型 ↔ 技能包 | 低 | 正交，仅指针关系 |

### 2.1 运行时真相（须写入文档与 UI）

**审核提示词解析顺序**（`LegalContractAuditRoleServiceImpl`）：

1. 合同快照 SkillPack（AUDIT）→ `chatRoleId` → `systemMessage`
2. 合同字段 `auditRoleId` / `reauditRoleId`
3. 内置默认角色名（首轮/二轮）

**法务 Agent 工具来源：** `legal_skill_pack.tool_names` ∩ `LegalSkillPackToolNames.ALLOWED`（**不读** `ai_chat_role.tool_ids`）。

**法务 RAG 来源：** `legal_contract_type.knowledgeId`（**不读** 聊天角色 `knowledgeIds`）。

---

## 3. 目标与非目标

### 3.1 目标

| ID | 目标 |
|----|------|
| G1 | 每类配置 **单一写入点**，UI 标明「去哪改」 |
| G2 | `PREFERRED_CLAUSE` 规则 **必须** 关联标准条款 |
| G3 | 技能包选工具 **对齐** `ai_tool` 法务工具目录（仍走白名单） |
| G4 | 新建合同 **默认** 走「类型 → 技能包 → 角色」，合同级提示词降为高级覆盖 |
| G5 | 合同类型表单展示 **配置链路说明**（知识库、技能包、规则） |

### 3.2 非目标

- 不合并数据库表（不删 `ai_chat_role` / `legal_skill_pack`）
- 不改 `LegalAiPolicyResolver` 冻结语义
- 不强制迁移历史合同的 `auditRoleId`
- 不在本 Spec 做「配置中枢」单页（留 P1）

---

## 4. 配置分层模型（SSOT）

```
┌─────────────────────────────────────────────────────────┐
│ L1 合规知识                                              │
│  标准条款库（正文范本） ←── 审核规则（检查逻辑 + 引用）    │
│  合同类型（分类 + knowledgeId + 默认技能包指针）          │
├─────────────────────────────────────────────────────────┤
│ L2 AI 编排                                               │
│  聊天角色（systemMessage，category=法务合同）            │
│  AI 技能包（scene + chatRoleId + toolNames + modelPolicy）│
├─────────────────────────────────────────────────────────┤
│ L3 平台目录                                              │
│  工具管理（ai_tool，Spring Bean 名 + 描述）              │
│  知识库（ai_knowledge，由合同类型绑定给法务链路）         │
└─────────────────────────────────────────────────────────┘
```

| Concern | SSOT | 禁止 |
|---------|------|------|
| 条款正文 | `legal_standard_clause.content` | 在 `ruleContent` 粘贴整段条款 |
| 检查逻辑 | `legal_audit_rule` | 在 `systemMessage` 写 Playbook 细则 |
| 审核人设/格式 | `ai_chat_role.systemMessage` | 在规则里写输出格式要求 |
| 场景工具集 | `legal_skill_pack.tool_names` | 在聊天角色 `tool_ids` 配法务 Agent 工具 |
| 法务 RAG | `legal_contract_type.knowledgeId` | 在聊天角色 `knowledgeIds` 配法务审核语料 |
| 类型默认 AI | `legal_contract_type.defaultSkillPackId*` | 在每条合同重复配全套 |

---

## 5. 分阶段实施

### P0 — 校验 + 引导 + 工具目录对齐（本迭代）

| ID | 交付物 |
|----|--------|
| P0-1 | 后端：`PREFERRED_CLAUSE` 保存时 `standardClauseId` 必填 |
| P0-2 | 后端：`GET /legal/skill-pack/legal-agent-tools` 返回 `ai_tool` 中 `legal_*` 且白名单内的工具 |
| P0-3 | 前端：技能包工具多选改调 API（静态列表作 fallback） |
| P0-4 | 前端：审核规则表单 — 推荐条款强制选标准条款；`ruleContent` 帮助文案 |
| P0-5 | 前端：合同类型字段 help（知识库 / 技能包 / 规则菜单跳转说明） |
| P0-6 | 前端：新建合同 — 审核提示词标为「高级覆盖」，说明技能包优先 |
| P0-7 | 单测：`LegalAuditRuleServiceImpl` 校验用例 |

### P1 — 配置中枢 ✅ 已实施

- 合同类型编辑弹窗：配置中枢面板 + 运行时解析预览
- 规则与条款四页：配置顺序 Banner
- 新建合同：高级提示词默认隐藏（`legal:contract:advanced` 或 `update` 权限可见）
- API：`GET /legal/contract-type/config-overview`、`GET /legal/contract-type/resolve-config`
- SQL：`sql/mysql/laby-legal-cfg001-permission.sql`

### P2 — 可观测（部分）

- `LegalContractAuditRoleServiceImpl` 增加 debug 解析来源日志

---

## 6. API 契约（P0 增量）

### 6.1 `GET /legal/skill-pack/legal-agent-tools`

**权限：** `legal:skill-pack:query`

**响应：**

```json
[
  { "name": "legal_search_paragraphs", "description": "检索合同段落", "registered": true },
  { "name": "legal_get_contract_meta", "description": "合同元信息", "registered": true }
]
```

**规则：**

- `name` ∈ `LegalSkillPackToolNames.ALLOWED`
- 优先取 `ai_tool`（`status=启用` 且 `name` 以 `legal_` 开头）；无 DB 记录时仍返回白名单项（`registered=false`）

### 6.2 审核规则保存校验

| ruleType | standardClauseId | matchPattern | ruleContent |
|----------|------------------|--------------|-------------|
| `PREFERRED_CLAUSE` | **必填** | 可选 | 禁止超过 200 字且含「条款」类长文（启发式，P0 仅必填 ID） |
| `MANDATORY_CLAUSE` / `FORBIDDEN_PATTERN` | 可选 | 建议必填 | 简短说明 |
| `CUSTOM_LLM` | 可选 | 忽略 | 检查清单，非条款全文 |

**错误码：** `1_050_000_061` — 「推荐标准条款规则必须关联标准条款」

---

## 7. 前端契约（P0）

### 7.1 审核规则表单

- `ruleType === 'PREFERRED_CLAUSE'` → `standardClauseId` `rules: 'required'`
- `ruleContent` help：「写检查要求，勿粘贴标准条款全文；推荐条款请选上方标准条款」

### 7.2 技能包表单

- `toolNames` 选项来源：`getLegalAgentTools()`
- help：「法务 Agent 实际可用工具；与 AI 控制台工具管理同步（仅 legal_*）」

### 7.3 合同类型表单

- `knowledgeId` help：「法务审核/Agent 检索用 RAG，不读聊天角色上的知识库」
- `defaultSkillPackIdAudit` help：「新建合同快照此包；提示词来自包内关联角色」

### 7.4 新建合同

- `auditRoleId` / `reauditRoleId` label 改为「审核提示词（高级覆盖）」
- help：「默认使用合同类型 → 技能包 → 聊天角色；仅特殊合同填写此项」

---

## 8. 验收标准

| # | 场景 | 期望 |
|---|------|------|
| AC-1 | 保存 `PREFERRED_CLAUSE` 且无 `standardClauseId` | 400 + 明确错误码 |
| AC-2 | 技能包工具下拉 | 展示与 `ai_tool` 中法务工具一致（≥白名单数量） |
| AC-3 | 新建合同不填 auditRoleId | 审核仍走 SkillPack 角色（现有行为不变） |
| AC-4 | 合同类型表单 | 可见 SSOT 帮助文案 |
| AC-5 | 单测 | P0-7 通过 |

---

## 9. 风险与回滚

| 风险 | 缓解 |
|------|------|
| 历史规则无 standardClauseId | 仅对新保存/更新校验；列表页标注待修复 |
| ai_tool 缺法务工具行 | API fallback 白名单，UI 仍可选 |
| 运营误以为删 ai_tool 即禁工具 | help 说明运行时以技能包为准 |

回滚：移除校验与 API；前端恢复静态 `TOOL_OPTIONS`。

---

## 10. 附录：代码映射

| 能力 | 类 / 路径 |
|------|-----------|
| 提示词解析 | `LegalContractAuditRoleServiceImpl` |
| 技能包解析 | `LegalSkillPackRegistry` · `LegalSkillPackSnapshotService` |
| 工具白名单 | `LegalSkillPackToolNames` |
| 规则 Playbook | `LegalDeterministicAuditEngine` · `LegalReviewPlanCompiler` |
| 规则 LLM 补充 | `LegalAuditContextServiceImpl` |
| 工具目录 | `AiToolService` · `ai_tool` |
