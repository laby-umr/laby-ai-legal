# laby-ai-legal — 交付文档索引

**laby-ai-legal**（Maven：`com.laby:laby-ai-legal`）法务合同 AI 审核平台的可交付设计文档与架构图。需求 Spec / 实施 Plan 在 `docs/superpowers/`。

## 目录结构

```
docs/
├── delivery/                 # 本目录 — 架构/系统设计、学习指南、架构图
│   ├── README.md
│   ├── 系统架构.png          # 系统架构图（交付预览）
│   ├── 技术架构.png          # 技术架构图（交付预览）
│   ├── 2026-06-03-legal-contract-architecture-design.md
│   ├── 2026-06-03-legal-contract-system-design.md
│   ├── 2026-06-13-agentscope-java-learning-guide.md
│   └── architecture/         # draw.io 源文件、提示词、生成脚本
│       ├── laby-admin-architecture.drawio
│       ├── laby-admin-drawio-prompts.md
│       └── generate-sketch-diagram.py
├── deploy/                   # 部署说明（Docker 编排见 script/docker/）
├── superpowers/              # Spec / Plan / 冒烟脚本
└── postman/                  # API Collection

script/docker/                # 统一 docker-compose.yml
```

## 架构图

| 预览 | 文件 | 说明 |
|------|------|------|
| 系统架构 | [系统架构.png](./系统架构.png) | 用户 → 前端 → 网关 → 业务域 → 存储/外部 |
| 技术架构 | [技术架构.png](./技术架构.png) | L5–L1 N-Tier 分层 + RAG / Agent 链路 |

![系统架构](./系统架构.png)

![技术架构](./技术架构.png)

**可编辑源文件**见 [architecture/](./architecture/README.md)：`laby-admin-architecture.drawio`（双页）、提示词与 `generate-sketch-diagram.py` 可重新生成布局；PNG 可在 draw.io / Next-AI-DrawIO MCP 中导出更新。

## 核心交付文档

| 文档 | 编号 | 说明 |
|------|------|------|
| [**AgentScope 2 Java 落地与学习指南**](./2026-06-13-agentscope-java-learning-guide.md) | Laby-AI-AGENTSCOPE-GUIDE-001 | AgentScope 架构、双路径、Tool/Middleware/Session、学习路线 |
| [架构设计说明书](./2026-06-03-legal-contract-architecture-design.md) | Laby-Legal-ARCH-001 | C4 架构、模块/数据/AI/BPM/部署、ADR（文字版，配图见上） |
| [系统设计说明书](./2026-06-03-legal-contract-system-design.md) | Laby-Legal-SDD-001 | 模块、流程时序、状态机、表结构、API、验收清单 |
| [架构图工作区](./architecture/README.md) | Laby-ARCH-DRAWIO-001 | draw.io 源文件、提示词、MCP 预览与重新生成 |

## 部署与运维

| 文档 | 说明 |
|------|------|
| [**Docker 统一编排**](../../script/docker/Docker-HOWTO.md) | 本地/测试环境：`docker compose --env-file docker.env up -d` |
| [OnlyOffice 集成说明](../deploy/onlyoffice-readme.md) | Document Server、插件挂载、`serviceCommand` 定位 |
| [生产就绪清单](../deploy/production-readiness-checklist.md) | 多实例、Redis Session、SSE 网关 |
| [生产就绪自检脚本](../deploy/verify-production-readiness.ps1) | 配合清单一键检查 |
| [SSE Nginx 样例](../deploy/nginx-admin-api-sse.conf) | 法务审核进度流式推送 |

```bash
# 日常开发：中间件全部拉起
cd script/docker && docker compose --env-file docker.env up -d

# 法务 OnlyOffice 审阅页
cd script/docker && docker compose --env-file docker.env up -d onlyoffice
```

## 关联需求与设计（历史）

| 文档 | 路径 |
|------|------|
| 产品 SRS | [../superpowers/specs/2026-06-01-legal-contract-review-full-srs.md](../superpowers/specs/2026-06-01-legal-contract-review-full-srs.md) |
| BPM 设计 v0.2 | [../superpowers/specs/2026-06-01-legal-contract-review-bpm-design.md](../superpowers/specs/2026-06-01-legal-contract-review-bpm-design.md) |
| 批注/导出设计 | [../superpowers/specs/2026-06-02-legal-contract-annotate-adopt-export-design.md](../superpowers/specs/2026-06-02-legal-contract-annotate-adopt-export-design.md) |
| Agent Spec | [../superpowers/specs/2026-06-03-legal-contract-agent-spec.md](../superpowers/specs/2026-06-03-legal-contract-agent-spec.md) |
| AI PPT 生成 Agent Spec | [../superpowers/specs/2026-06-13-ai-ppt-generation-agent-spec.md](../superpowers/specs/2026-06-13-ai-ppt-generation-agent-spec.md) |
| AI PPT CodeX 工作台 Spec | [../superpowers/specs/2026-06-15-ai-ppt-codex-redesign-spec.md](../superpowers/specs/2026-06-15-ai-ppt-codex-redesign-spec.md) |
| 平台演进总体规划 | [../superpowers/specs/2026-06-04-legal-contract-platform-evolution-spec.md](../superpowers/specs/2026-06-04-legal-contract-platform-evolution-spec.md) |
| OnlyOffice 多格式文档平台（E10） | [../superpowers/specs/2026-06-04-legal-onlyoffice-document-platform-spec.md](../superpowers/specs/2026-06-04-legal-onlyoffice-document-platform-spec.md) |
| E10 Phase1 / Phase2 Plan | [../superpowers/plans/2026-06-04-legal-e10-onlyoffice-phase1.md](../superpowers/plans/2026-06-04-legal-e10-onlyoffice-phase1.md) · [phase2](../superpowers/plans/2026-06-04-legal-e10-onlyoffice-phase2.md) |
| E10 实施摘要 | [../superpowers/plans/2026-06-04-legal-e10-phase-summary.md](../superpowers/plans/2026-06-04-legal-e10-phase-summary.md) |
| Wave1～Wave5 演进摘要 | [Wave1](../superpowers/plans/2026-06-04-legal-evolution-wave1-parallel-orchestration.md) · [Wave2](../superpowers/plans/2026-06-04-legal-evolution-wave2-summary.md) · [Wave3](../superpowers/plans/2026-06-04-legal-evolution-wave3-summary.md) · [Wave4](../superpowers/plans/2026-06-04-legal-evolution-wave4-summary.md) · [Wave5](../superpowers/plans/2026-06-04-legal-evolution-wave5-summary.md) |

## SQL 部署顺序

在目标库按顺序执行 `sql/mysql/` 下脚本（均可重复执行）：

```
1. ruoyi-vue-pro.sql    # 芋道基础库（系统 / 基础设施 / BPM / AI 等）
2. quartz.sql           # 定时任务（可选）
3. laby-init.sql        # 法务 + AI 知识库增量（合并脚本，含菜单与权限）
```

> **高级提示词权限（CFG-001）**：`laby-init.sql` 已包含 `legal:contract:advanced` 权限段；执行后需重新登录或刷新权限缓存。

## Playbook 黄金集评测（E7）

```bash
mvn -pl laby-module-legal test "-Dtest=LegalPlaybookEvalRunnerTest"
```

- 数据集：`laby-module-legal/src/test/resources/eval/playbook-cases.json`（10 条）
- CI 工作流：`.github/workflows/legal-eval.yml`（默认 passRate = 100%）
- 可调阈值：`-Dlegal.eval.minPassRate=0.95`（常量见 `LegalEvalConstants`）
- 报告：`laby-module-legal/target/eval-reports/playbook-eval-report.json`
