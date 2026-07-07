# OPT-002 — 架构收敛 Spec

| 字段 | 值 |
|------|-----|
| **状态** | **Implemented（Wave 1 + Wave 2）** |
| **前置** | OPT-001 Wave 1～3 + 5 已完成 |
| **范围** | Wave 4（OPT-001 backlog） |

## 目标

1. **W4-2** Legal 模块不再直依赖 `AiChatMessageMapper` / `AiChatMessageDO`
2. **W4-4** Legal + AgentScope 配置索引文档化
3. **W4-1** BPM 审核非阻塞（ReceiveTask + signal）— 分期，需 BPMN 变更

## W4-2 LegalAiChatFacade

| 类 | 包 | 职责 |
|----|-----|------|
| `LegalAiChatFacade` | `service.ai` | Legal 读 AI 对话消息的防腐层 |
| `LegalAiChatFacadeImpl` | `service.ai` | 委托 `AiChatMessageService` |

**首批方法：** `listLatestUserAttachmentUrls(Long conversationId)`

**迁移：** `LegalOrchestrationAttachmentService` 改注入 Facade。

## W4-4 配置索引

在 `laby-project/reference.md` 增加 `Legal*Properties` + `AgentScopeProperties` 对照表。

## W4-1 BPM 非阻塞（已实现）

```text
secondRoundGateway → aiRound2Enqueue (ServiceTask, enqueue only)
                  → awaitAiRound2 (ReceiveTask)
                  → reviewRound2
```

| 组件 | 职责 |
|------|------|
| `LegalAiAuditDelegate` | ServiceTask 仅 `enqueueAuditForBpm` |
| `LegalAiAuditReceiveListener` | ReceiveTask 进入时若已终态则 trigger |
| `LegalContractBpmAuditSignalService` | Consumer 完成后 `triggerTask(awaitAiRound2)` |
| `LegalContractAuditConsumer` | MQ 执行 `executeAudit(failFast=false)` |

**已删除：** `LegalAuditCompletionWaiter` 线程轮询。

## 验收

| # | 项 |
|---|-----|
| AC-1 | `rg AiChatMessageMapper laby-module-legal` 无命中 |
| AC-2 | `mvn -pl laby-module-legal test` 通过 |
| AC-3 | 配置索引表已写入 reference.md |
| AC-4 | BPM 二轮路径无 `Thread.sleep` 轮询等待审核 |
