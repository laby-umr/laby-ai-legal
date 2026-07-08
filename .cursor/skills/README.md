# Laby Skills — 四类体系

**Rules（路由）**：`.cursor/rules/` — 薄层，只告诉 Agent **按什么顺序读哪些 Skill**；规范正文仍在下方 4 个 Skill 中。

本目录 **仅 4 个 Skill**，按性质分组，避免碎片化。

```
.cursor/skills/
├── laby-global/      ① 全局规范（FQN、SQL/utf8mb4、Shell 禁止乱码改源码、Git 提交…）
├── laby-project/     ② 本项目 laby-admin（模块、接法、文档路径）
├── laby-role/        ③ 业务角色（legal / ai / frontend）
└── laby-workflow/    ④ 外部流程（Superpowers + 冒烟/E2E/Postman）
```

## 怎么用

| 场景 | 读哪些 Skill |
|------|----------------|
| 新功能 / 大改 | `laby-workflow` → Superpowers → 编码时 **global + project + role** |
| 修 Bug | `laby-workflow` Hotfix → **global + project + role** |
| 只写代码 | **global**（强制）+ **project** + **role**（按模块） |
| 纯问答 | 按需 **project** / **role**，无改代码可不读 workflow |

## 优先级

```
用户指令 > laby-global > laby-project / laby-role > Superpowers 默认
```

- **global**：换项目也能用的团队规范  
- **project**：只有 laby-admin 才有的结构和技术接法  
- **role**：同一项目里不同业务线的开发视角  
- **workflow**：不碰 Superpowers 本体，只定义衔接与交付门禁  

## 外部插件（不在本目录）

Superpowers 自带，通过 Cursor 插件加载：

- `brainstorming` → `writing-plans` → `executing-plans` → `finishing-a-development-branch`
- `systematic-debugging`（Hotfix）
- `verification-before-completion`、`using-git-worktrees` 等

详见 **`laby-workflow/SKILL.md`**。

## 配套资产（非 Skill）

| 路径 | 用途 |
|------|------|
| `docs/postman/` | Postman Collection / Environment |
| `docs/superpowers/scripts/` | 冒烟脚本 |
| `docs/superpowers/specs|plans/` | Spec / Plan |

## 角色速查

| 改动 | 角色（laby-role） |
|------|-------------------|
| `laby-module-legal/**` | legal-backend |
| `laby-module-ai/**` | ai-backend |
| `laby-ui/**` | frontend |

## 后续可扩展

- 新 **角色**：在 `laby-role/SKILL.md` 加一章（如 `infra-backend`），不新建目录  
- 新 **全局规范**：写入 `laby-global/reference.md`  
- 新 **项目接法**：写入 `laby-project/reference.md`  
- CI 门禁：`.github/workflows/code-quality-gate.yml`（FQN 检查）；`baseline-smoke.ps1` 仍须本地 laby-server
