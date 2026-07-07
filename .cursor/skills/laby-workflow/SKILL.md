---
name: laby-workflow
description: >-
  EXTERNAL development workflow for laby-admin: Superpowers plugin phases (brainstorming, plans,
  execute, finish) PLUS laby extensions (smoke, E2E, Postman sync, hotfix path). Use at task start
  for features/bugs; combine with laby-global, laby-project, laby-role when coding.
---

# 开发流程（外部 Superpowers + 交付扩展）

**性质：** **流程 / 第三方** — Superpowers 插件 + 本仓库交付补全（冒烟、E2E、Postman）。  
**不替代：** Superpowers 自带 Skill（`brainstorming`、`writing-plans` 等），本 Skill 说明 **如何衔接**。

**Announce:** "Using laby-workflow."

**插件：** `.cursor/settings.json` → `superpowers.enabled: true`

---

## 与其它 Skill 的关系

```
任务开始 → laby-workflow（选流程）
写代码   → laby-global（必遵）+ laby-project + laby-role（选一个）
```

---

## 任务分类 → 流程

| 类型 | 流程 |
|------|------|
| 新功能 / 增强 / 重构 | **全链路**（下表 1～10） |
| 修 Bug | **Hotfix**（下节） |
| 纯问答 | 不强制流程；若要改代码则回到全链路/Hotfix |

---

## 全链路（功能开发）

| # | 执行 | 产出 |
|---|------|------|
| 1 | `superpowers:brainstorming` | `docs/superpowers/specs/YYYY-MM-DD-*-spec.md` |
| 2 | `superpowers:writing-plans` | `docs/superpowers/plans/YYYY-MM-DD-*-plan.md` |
| 3 | `superpowers:using-git-worktrees` | 隔离分支（用户未禁用时） |
| 4 | `superpowers:executing-plans` | 代码；编码时 **laby-global + laby-project + laby-role** |
| 5 | `superpowers:verification-before-completion` | `mvn -pl {module} test` |
| 6 | **冒烟**（本节 §Smoke） | `docs/superpowers/scripts/{feature}-smoke.ps1` exit 0 |
| 7 | **E2E**（本节 §E2E） | Plan/spec 清单勾选 |
| 8 | **Postman**（本节 §Postman） | `docs/postman/` 更新 + 用户 Import |
| 9 | `superpowers:finishing-a-development-branch` | merge / PR |

**Plan 必须含** 交付验证 Task（单测 + smoke + e2e + postman）。

**finishing 前硬门禁：** 单测过、smoke 过、API 变更则 Postman 已更、主流程 E2E 已勾。

---

## Hotfix（修 Bug）

```
1. superpowers:systematic-debugging   根因证据，禁止猜修
2. laby-global + laby-project + laby-role
3. 最小 diff + 回归单测
4. mvn -pl {module} test
5. 冒烟（受影响 API；无则补脚本）
6. API 变更 → Postman
7. finishing-a-development-branch
```

需改架构时 → 回到全链路 brainstorming。

可选迷你 plan：`docs/superpowers/plans/YYYY-MM-DD-{bug}-hotfix-plan.md`

---

## §PR 门禁（本地 + CI）

| 检查 | 命令 / 位置 |
|------|-------------|
| 内联 FQN | `.\docs\superpowers\scripts\check-fqn.ps1`（CI：`.github/workflows/code-quality-gate.yml`） |
| 模块单测 | `mvn -pl laby-module-legal,laby-module-ai test` |
| 基线冒烟 | `.\docs\superpowers\scripts\baseline-smoke.ps1`（需本地 laby-server:48080） |

---

## §Smoke 冒烟

**时机：** 单测后、E2E/Postman 前。

**脚本：**

```
docs/superpowers/scripts/_lib/smoke-common.ps1   # 登录、断言
docs/superpowers/scripts/template-smoke.ps1      # 复制模板
docs/superpowers/scripts/baseline-smoke.ps1      # 基线
docs/superpowers/scripts/{feature}-smoke.ps1     # 本需求
```

**运行：**

```powershell
.\docs\superpowers\scripts\baseline-smoke.ps1
# 或
.\docs\superpowers\scripts\{feature}-smoke.ps1
```

**要求：** 登录 + 本需求核心 API 返回 `code=0`；输出 `SMOKE OK`；server 默认 `http://localhost:48080`。

---

## §E2E

**与 Smoke：** Smoke = 单接口可达；E2E = 多步业务闭环。

在 Plan 或 Spec 写 `## E2E Checklist`：

```markdown
- [ ] E1: {步骤} → 期望 {状态/DB}
- [ ] E2: 失败路径 → errorCode
- [ ] E3: 前端 {路由}（如适用）
```

**示例（legal）：** 上传 docx → PARSED → AI_AUDITED → 意见入库  
**示例（ai）：** 上传 PDF → segment + vector → 召回诊断有结果

---

## §Postman

**文件：**

```
docs/postman/laby-admin.postman_collection.json
docs/postman/laby-admin.postman_environment.local.json
docs/postman/README.md
```

**API 变更后必做：**

1. 更新 Collection（Folder 按 Legal/AI/System）
2. 告知用户 **Re-Import**（Postman 不自动跟 Git）：

```
Postman → Import → docs/postman/laby-admin.postman_collection.json
Import → laby-admin.postman_environment.local.json
先跑 Login → 再跑新接口
```

---

## 子 Agent

被派发执行 Plan 中某一 Task：跳过 brainstorming；仍须 **laby-global** + 对应 **laby-role**。

---

## TodoWrite 模板（全链路）

- [ ] Spec / Plan 路径
- [ ] 实现 + 单测
- [ ] Smoke
- [ ] E2E（如适用）
- [ ] Postman（API 变更）
- [ ] finishing
