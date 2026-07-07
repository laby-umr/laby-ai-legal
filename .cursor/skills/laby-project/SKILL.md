---
name: laby-project
description: >-
  laby-admin PROJECT guide: repo layout, modules (legal/ai/ui/framework), Ruoyi patterns, AgentScope/
  OnlyOffice/Qdrant as used in THIS repo, docs paths, ErrorCode segments, local dev. Use together
  with laby-global and laby-role when working in this codebase.
---

# 项目指南（laby-admin）

**性质：** **本仓库专属** — 模块划分、技术栈接法、文档路径。  
**不写：** 通用 P3C（见 `laby-global`）；业务开发视角（见 `laby-role`）。

---

## 仓库结构

```
laby-server/              启动入口 application-local.yaml
laby-framework/           Ruoyi 封装、starter
laby-module-legal/        法务合同、审核、OnlyOffice
laby-module-ai/           Chat、RAG、AgentScope
laby-ui/                  Vben web-ele 前端
sql/mysql/                增量脚本 laby-{module}-{feature}.sql
docs/superpowers/         spec / plan
docs/delivery/            交付文档索引 README.md
docs/deploy/              OnlyOffice / 生产清单 / nginx SSE
script/docker/            统一 docker-compose.yml
docs/postman/             API Collection
docs/superpowers/scripts/ 冒烟脚本
```

## 模块与 ErrorCode

| 模块 | 路径 | ErrorCode |
|------|------|-----------|
| legal | `laby-module-legal` | `1-050-000-000` |
| ai | `laby-module-ai` | 见模块 `ErrorCodeConstants` |

## 本项目的 Ruoyi 用法

- 返回 `CommonResult`；分页 `PageParam`/`PageResult`
- Mapper `BaseMapperX`；异常 `throw exception(ErrorCodeConstants.X)`
- 租户 `tenant_id` + `TenantUtils`；权限 `@PreAuthorize`
- 本地 Postman：`captcha.enable: false`（application-local.yaml）

## 本项目的技术接法（摘要）

### AgentScope

- `io.agentscope.*` **仅** `framework/agentscope/**`、Tool、测试
- 普通 LLM：`AiLlmClient`；Agent：`AiChatAgentScopeConfig` / `LegalAgentScopeConfig`
- 配置统一 `AgentScopeProperties`
- 自检：`rg "[^/]\bio\.agentscope\.[a-z]" --glob "*.java" laby-module-ai laby-module-legal`

### OnlyOffice

- Compose：`script/docker/docker-compose.yml`（`docker compose --env-file docker.env up -d onlyoffice`）
- 书签/段落定位与 `LegalContractDocxRenderUtil` 同步
- Spec：`docs/superpowers/specs/2026-06-04-legal-onlyoffice-*`

### RAG（本仓库实现）

`Dense(Qdrant) + Sparse(MySQL FULLTEXT) → RRF → Rerank`（Sparse 非 BM25）

## 文档约定

| 类型 | 路径 |
|------|------|
| Spec | `docs/superpowers/specs/YYYY-MM-DD-*.md` |
| Plan | `docs/superpowers/plans/YYYY-MM-DD-*.md` |
| 交付 | `docs/delivery/`（索引见 [README.md](../../docs/delivery/README.md)） |
| 部署 | `docs/deploy/`（Docker 编排见 `script/docker/Docker-HOWTO.md`） |

## 详细索引

见 [reference.md](reference.md)
