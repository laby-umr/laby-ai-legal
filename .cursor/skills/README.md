# Laby Skills — 四类体系

> **总流程入口**：[`.cursor/README.md`](../README.md)（Rules / Hooks / MCP / 验证闭环）

本目录存放 **规范与流程正文**。Rules 只负责路由到这里，**不要**在 Rules 里重复以下内容。

---

## 四个 Skill

```
.cursor/skills/
├── laby-workflow/    ④ 整体流程（Superpowers + 冒烟/E2E/Postman）
├── laby-global/      ① 全局规范（FQN、SQL/utf8mb4、Shell 禁止乱码改源码、Git…）
├── laby-project/     ② 本项目 laby-ai-legal（模块、接法、文档路径）
└── laby-role/        ③ 业务角色（legal / ai / frontend）
```

| Skill | 文件 | 何时读 |
|-------|------|--------|
| workflow | [laby-workflow/SKILL.md](laby-workflow/SKILL.md) | 新功能、大改、修 Bug（有代码变更） |
| global | [laby-global/SKILL.md](laby-global/SKILL.md) | **写 Java/SQL/Vue 必遵** |
| project | [laby-project/SKILL.md](laby-project/SKILL.md) | 任何本仓库改动 |
| role | [laby-role/SKILL.md](laby-role/SKILL.md) | 按主改动模块选一个角色 |

---

## 怎么用

| 场景 | 读哪些 |
|------|--------|
| 新功能 / 大改 | workflow → Superpowers → **global + project + role** |
| 修 Bug | workflow Hotfix → **global + project + role** |
| 只写代码 | **global** + **project** + **role** |
| 纯问答 | 按需 project / role；无改代码可不读 workflow |
| 跨模块 / >3 文件 | 先 Plan Mode，再按上表 |

**叠加顺序：**

```
用户指令 → laby-global → laby-project → laby-role（选中章节）
```

---

## 与 Rules / Hooks / MCP 的关系

| 机制 | 路径 | 职责 |
|------|------|------|
| Rules | `.cursor/rules/` | 薄路由（glob 触发读哪个 role / MCP） |
| **Skills（本目录）** | `.cursor/skills/` | 规范与流程 **唯一正文** |
| Hooks | `.cursor/hooks.json` | commit 前 FQN、禁止 `sql/` |
| MCP drawio | `.cursor/mcp.json` | 架构图 |
| MCP postman | `.cursor/mcp.json` + `POSTMAN_API_KEY` | API 云端验证；Git 主源见 `docs/postman/` |
| MCP Browser | Cursor 内置 | 前端 E2E（见 `rules/14-mcp-browser.mdc`） |

---

## 角色速查（laby-role）

| 主改动路径 | 角色 |
|------------|------|
| `laby-module-legal/**` | legal-backend |
| `laby-module-ai/**` | ai-backend |
| `laby-ui/**` | frontend |

---

## 外部插件（Superpowers）

通过 `.cursor/settings.json` 加载，workflow 说明如何衔接：

- `brainstorming` → `writing-plans` → `executing-plans` → `finishing-a-development-branch`
- `systematic-debugging`（Hotfix）
- `verification-before-completion`、`using-git-worktrees`

---

## 配套资产（非 Skill）

| 路径 | 用途 |
|------|------|
| `docs/superpowers/specs/` | 需求 Spec |
| `docs/superpowers/plans/` | 实施 Plan |
| `docs/superpowers/scripts/` | Smoke、FQN（`check-fqn.ps1`） |
| `docs/postman/` | Postman Collection |

---

## 扩展方式

- 新 **角色**：在 `laby-role/SKILL.md` 加一章，不新建目录
- 新 **全局规范**：`laby-global/reference.md`
- 新 **项目接法**：`laby-project/reference.md`
- 新 **领域 Skill**（可选）：如 onlyoffice / rag，在 role 里加链接
