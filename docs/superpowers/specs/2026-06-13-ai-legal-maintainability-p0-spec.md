# AI + 法务可维护性收敛 Spec（MAINT-001）

| 属性 | 值 |
|------|-----|
| **文档编号** | Laby-AI-Legal-MAINT-001 |
| **版本** | v1.0 |
| **日期** | 2026-06-13 |
| **状态** | **Approved for Implementation** |
| **模块** | `laby-module-ai` · `laby-module-legal` |
| **上游** | 架构审查（Chat/Segment God Class、双路径 RAG、法务 resolveModel 重复、错误码冲突） |

---

## 1. 执行摘要

本迭代聚焦 **P0：低风险、立竿见影** 的可维护性修复，不拆 God Class、不改 BPM 阻塞模型（留 P1）。

| ID | 交付 | 模块 |
|----|------|------|
| P0-1 | 修复法务重复错误码 `1_050_000_056` | legal |
| P0-2 | 抽取 `LegalAiModelResolver`，统一 5 处 `resolveModel` | legal |
| P0-3 | 移除知识库 **Legacy RAG** 路径，Universal RAG 为唯一检索实现 | ai |
| P0-4 | 检索测试 API 改用 `searchKnowledgeSegmentWithDiagnostics` 真实诊断 | ai |
| P0-5 | 单测：`LegalAiModelResolver`、保留既有 RRF 单测 | both |

**预估工期：** 0.5～1 天。

---

## 2. 背景与问题

### 2.1 AI 知识库双路径 RAG

`AiKnowledgeSegmentServiceImpl` 在 `knowledge-retrieval.enabled=true` 时走 Universal RAG，否则走 legacy `searchHits()`（Dense + 可选 DashScope Rerank）。两套路径导致：

- RRF 分与 cosine 阈值混用（已引发「召回全 0」）
- `enrichSearchResults` 重复
- 检索测试页 `RecallDiagnosticsBuilder` 伪造 dense/sparse 计数，与聊天链路不一致

### 2.2 法务模型解析重复

`resolveModel(Long modelId)` 在 5 个 Service 中复制粘贴，默认回退逻辑分散。

### 2.3 错误码冲突

`CONTRACT_OPINION_PENDING_NOT_DISPOSED` 与 `CONTRACT_DELIVERABLE_INVALID` 共用 `1_050_000_056`。

---

## 3. 目标与非目标

### 3.1 目标

| ID | 目标 |
|----|------|
| G1 | 知识库检索 **单一路径**（Universal RAG） |
| G2 | 管理端检索测试展示 **真实** dense/sparse/fused/rerank 诊断 |
| G3 | 法务 AI 模型解析 **单一类** |
| G4 | 法务错误码 **全局唯一** |

### 3.2 非目标（P1+）

- 拆分 `AiChatMessageServiceImpl` / `LegalContractServiceImpl`
- BPM `waitUntilAuditSettled` 改事件驱动
- `orchestrator` vs `orchestration` 包重命名
- AI Facade 替代法务对 `AiChatMessageMapper` 的直接访问

---

## 4. 技术方案

### 4.1 P0-1 错误码

- `CONTRACT_OPINION_PENDING_NOT_DISPOSED` 保持 `1_050_000_056`
- `CONTRACT_DELIVERABLE_INVALID` 改为 `1_050_000_057`（当前未占用）

### 4.2 P0-2 LegalAiModelResolver

```text
com.laby.module.legal.service.ai.LegalAiModelResolver
  - resolveChatModel(Long modelId): AiModelDO
  - isModelFallback(Long requestedModelId, Long resolvedModelId): boolean
```

调用方替换私有 `resolveModel` / `isModelFallback`：

- `LegalAiAuditServiceImpl`
- `LegalAuditKernelImpl`
- `LegalContractChatServiceImpl`
- `LegalContractAgentServiceImpl`
- `LegalContractAgentRebuildService`

### 4.3 P0-3 移除 Legacy RAG

- `searchKnowledgeSegment` / `searchKnowledgeSegmentWithDiagnostics` 均委托 `AiKnowledgeRetrievalService`
- 删除 `searchHits()`、`RERANK_RETRIEVAL_FACTOR`、`DashScopeRerankClient` 在 SegmentService 中的注入
- `knowledgeRetrievalService == null` 时抛出 `KNOWLEDGE_RETRIEVAL_DISABLED`（`1_040_009_116`）
- 默认配置 `laby.ai.knowledge-retrieval.enabled=true` 不变

### 4.4 P0-4 检索测试 API

`AiKnowledgeSegmentController.search`：

- 调用 `segmentService.searchKnowledgeSegmentWithDiagnostics(reqBO)`
- 诊断 VO 使用 `RecallDiagnosticsBuilder.toVo(realDiagnostics)`
- 移除 `RecallDiagnosticsBuilder.start/finish` 伪造逻辑

---

## 5. 验收标准

| # | 验收项 |
|---|--------|
| AC-1 | `mvn -pl laby-module-ai,laby-module-legal test` 通过 |
| AC-2 | 知识库检索测试页 dense/sparse 与后端日志 `KnowledgeRetrieval` 一致 |
| AC-3 | `ErrorCodeConstants` 法务段无重复 code |
| AC-4 | 5 个法务类无私有 `resolveModel` 方法 |

---

## 6. 风险与回滚

| 风险 | 缓解 |
|------|------|
| 关闭 `knowledge-retrieval.enabled` 的环境无法检索 | 抛明确错误码；文档注明必须开启 |
| 交付物错误码变更 | 057 为新码，前端若硬编码 056 需同步（当前无） |

回滚：恢复 SegmentService legacy 分支 + 还原错误码（不推荐）。
