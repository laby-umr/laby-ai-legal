# laby-ai-legal · Cursor 开发体系

本目录是 **laby-ai-legal** 在 Cursor 中的 AI 协作配置：Rules 路由、Skills 规范、Hooks 门禁、MCP 工具、Superpowers 流程。

> 新人 / Agent 入口：**先读本文件**，再按任务类型进入对应 Skill。

---

## 总览图

```
用户任务
   │
   ▼
Rules（.cursor/rules/）          ← 薄层：告诉 Agent 读哪些 Skill、走哪条路径
   │
   ├── laby-workflow              ← ④ 整体流程（Spec → Plan → 实现 → 验证）
   ├── laby-global                ← ① 全局规范（FQN、SQL、Git…）
   ├── laby-project               ← ② 项目接法（模块、Docker、文档路径）
   └── laby-role                  ← ③ 业务视角（legal / ai / frontend）
   │
   ▼
编码 + 验证
   ├── Hooks（commit 前 FQN、禁 sql/）
   ├── Smoke / E2E / Postman（见 workflow）
   └── MCP（drawio 架构图、Browser 前端 E2E）
```

---

## 目录结构

```
.cursor/
├── README.md                 ← 本文件（总流程）
├── settings.json             ← 插件：Superpowers / Linear / Harness / shadcn
├── mcp.json                  ← MCP：drawio、postman
├── hooks.json                ← Agent Shell 门禁
├── hooks/before-shell.ps1
├── rules/                    ← 路由 Rules（.mdc）
│   ├── 00-dev-router.mdc     ← Always：总路由
│   ├── 10-role-frontend.mdc  ← laby-ui/**
│   ├── 11-role-legal-backend.mdc
│   ├── 12-role-ai-backend.mdc
│   ├── 13-mcp-drawio.mdc
│   ├── 14-mcp-browser.mdc
│   └── 15-mcp-postman.mdc
└── skills/                   ← 规范与流程正文
    ├── README.md
    ├── laby-workflow/
    ├── laby-global/
    ├── laby-project/
    └── laby-role/
```

根目录还有：

| 文件 | 作用 |
|------|------|
| `.cursorignore` | 索引排除 node_modules / target / sql 等 |
| `docs/superpowers/` | Spec / Plan |
| `docs/postman/` | API Collection |
| `docs/superpowers/scripts/` | Smoke、FQN 检查 |

---

## 按任务怎么开始

| 任务 | 第一步 | 然后 |
|------|--------|------|
| **新功能 / 大改** | 读 `skills/laby-workflow/SKILL.md` 全链路 | Superpowers Spec/Plan → global + project + role |
| **修 Bug** | workflow Hotfix | systematic-debugging → 最小 diff + 回归单测 |
| **只改后端 legal** | `laby-role` → legal-backend | + global + project |
| **只改后端 ai** | `laby-role` → ai-backend | + global + project |
| **只改前端** | `laby-role` → frontend | + global + project；页面流程用 Browser MCP |
| **画架构图** | `rules/13-mcp-drawio.mdc` | drawio MCP + `docs/delivery/architecture/` |
| **纯问答** | 按需 project / role | 无改代码可不读 workflow |

**跨模块或 >3 文件**：先 **Plan Mode** 写 Spec/Plan，再 Agent 实现。

---

## 四层 Skills（正文在这里）

| 层 | Skill | 管什么 |
|----|-------|--------|
| ④ 流程 | [laby-workflow](skills/laby-workflow/SKILL.md) | Spec/Plan、单测、Smoke、E2E、Postman、finishing |
| ③ 业务 | [laby-role](skills/laby-role/SKILL.md) | legal-backend / ai-backend / frontend |
| ② 项目 | [laby-project](skills/laby-project/SKILL.md) | 模块路径、OnlyOffice、Docker、ErrorCode |
| ① 规范 | [laby-global](skills/laby-global/SKILL.md) | Ruoyi 分层、FQN、SQL utf8mb4、Git |

优先级：`用户指令 > laby-global > laby-project / laby-role > Superpowers`

---

## Rules vs Skills vs Hooks

| 机制 | 职责 | 会不会重复写规范 |
|------|------|------------------|
| **Rules** | 路由：读哪个 Skill、哪条 glob | 否，只指路 |
| **Skills** | 规范与流程正文 | 是，唯一真相源 |
| **Hooks** | 自动化拦截（commit、sql/、FQN） | 否，只执行检查 |
| **MCP** | 外部工具能力 | 否 |

---

## MCP

| 来源 | 用途 | 配置 |
|------|------|------|
| **drawio** | 架构图生成/预览 | `.cursor/mcp.json` |
| **postman** | Collection 云端管理、跑请求验证 | `.cursor/mcp.json` + `POSTMAN_API_KEY` |
| **Browser** | 前端 E2E（登录、合同页、OnlyOffice） | Cursor 内置，见 `rules/14-mcp-browser.mdc` |

Postman **Git 主源**：`docs/postman/laby-admin.postman_collection.json`；MCP 为辅，见 `rules/15-mcp-postman.mdc`。

---

## Hooks 门禁

`.cursor/hooks/before-shell.ps1` 在 Agent 执行 Shell 时：

- 禁止 `git add sql/`
- 禁止 `git push --force` 到 main/master
- `git commit` 前：staged 不得含 `sql/`；跑 `docs/superpowers/scripts/check-fqn.ps1`

修改 Hooks 后需 **重启 Cursor** 生效。

---

## 验证闭环（workflow 摘要）

```
mvn -pl {module} test
  → Smoke（docs/superpowers/scripts/*-smoke.ps1）
  → E2E 清单（Spec/Plan 里勾选；前端用 Browser MCP）
  → API 变更 → 更新 docs/postman/ + 用户 Re-Import
  → finishing-a-development-branch
```

---

## 插件（settings.json）

| 插件 | 用途 |
|------|------|
| Superpowers | brainstorming / writing-plans / executing-plans |
| Linear | Issue ↔ Spec 对齐（可选） |
| Harness | CI/CD（可选） |
| shadcn | 组件（前端可选） |

---

## 相关文档

- 交付索引：[docs/delivery/README.md](../docs/delivery/README.md)
- 项目 README：[README.md](../README.md)
