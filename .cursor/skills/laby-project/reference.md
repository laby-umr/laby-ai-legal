# laby-admin 项目参考

## Legal 包职责：`orchestrator` vs `orchestration`

| 包路径 | 职责 | 典型入口 |
|--------|------|----------|
| `service.orchestrator` | 合同审核 **Playbook + LLM Pipeline**（正式审核、预览合并） | `LegalAiOrchestrator`、`LegalAiAuditPipelineCommand` |
| `service.orchestration` | 多文件 **Agent 会话**、合同分类、预览审核、编排确认创建 | `LegalOrchestrationSessionService`、`LegalOrchestrationPreviewAuditService` |

**边界约定：**

- `orchestrator`：单合同审核链路，被 `LegalAuditKernel` / `LegalAiAuditServiceImpl` 调用
- `orchestration`：对话式多步编排，不直接写 opinion/report；创建合同走 `LegalContractCreateService`
- 跨模块读 AI 消息：走 `LegalAiChatFacade`（`service.ai`），禁止直依赖 `AiChatMessageMapper`

## Legal / AgentScope 配置索引

| 前缀 | Properties 类 | 模块 | 用途 |
|------|----------------|------|------|
| `laby.legal.audit` | `LegalAuditProperties` | legal | 审核并发、队列 |
| `laby.legal.chat` | `LegalChatMemoryProperties` | legal | 合同问答记忆 |
| `laby.legal.orchestration` | `LegalOrchestrationProperties` | legal | 多文件编排 Agent |
| `laby.legal.memory` | `LegalContractMemoryProperties` | legal | 合同情景记忆 |
| `laby.legal.format-convert` | `LegalFormatConvertProperties` | legal | DOC/PDF 转换 |
| `laby.legal.onlyoffice` | `LegalOnlyOfficeProperties` | legal | OnlyOffice 文档平台 |
| `laby.legal.playbook` | `LegalPlaybookProperties` | legal | 确定性 Playbook 开关 |
| `laby.ai.agentscope` | `AgentScopeProperties` | ai | Harness 会话、Compaction、重试 |

**关系：** Legal Agent/Chat 通过 `AgentScopeModelFactory` 读 `AgentScopeProperties`；法务合同 Chat 另读 `LegalChatMemoryProperties`。

## BPM 二轮 AI 审核（非阻塞）

```text
意见复核 needSecondRound=true
  → BPM: aiRound2Enqueue (LegalAiAuditDelegate → enqueueAuditForBpm)
  → awaitAiRound2 (ReceiveTask，阻塞流程线程但不占 worker 轮询)
  → MQ Consumer executeAudit 完成
  → LegalContractBpmAuditSignalService.triggerTask(awaitAiRound2)
  → reviewRound2
```

**竞态：** ReceiveTask 进入时 `LegalAiAuditReceiveListener` 若 progress 已 COMPLETED/FAILED 会立即 trigger。

## 关键类索引

### Legal

- `LegalAiChatFacade` — 读 AI 对话消息（防腐层）
- `LegalContractBpmAuditSignalService` — AI 审核终态唤醒 ReceiveTask
- `LegalAiAuditServiceImpl` — 审核入口
- `LegalAuditKernelImpl` — 内核
- `LegalContractProcessStarter` — afterCommit 管道
- `LegalContractAuditConsumer` — 二轮 Stream 审核

### AI

- `AiKnowledgeRetrievalServiceImpl` — RAG 召回
- `RrfFusion` — RRF 融合
- `AiChatAgentScopeConfig` — Chat Agent

## AgentScope Middleware 模板

```java
@Override
public Flux<AgentEvent> onActing(Agent agent, ActingInput input,
                                 Function<ActingInput, Flux<AgentEvent>> next) {
    return next.apply(input);
}
```

## 部署相关

- 文档索引：`docs/delivery/README.md`
- Docker：`script/docker/Docker-HOWTO.md`
- OnlyOffice：`cd script/docker && docker compose --env-file docker.env up -d onlyoffice`
- SSE nginx：`docs/deploy/nginx-admin-api-sse.conf`
- 学习：`docs/delivery/2026-06-13-agentscope-java-learning-guide.md`
