# OPT-002 实施计划

> **For agentic workers:** Use superpowers:executing-plans task-by-task.

**Goal:** Legal 防腐层 Facade + 配置文档；BPM 异步单独立项 Wave 2。

**Architecture:** `LegalAiChatFacade` 委托 `AiChatMessageService`；BPM 改动画后续 PR。

---

## Wave 1 — Facade + 配置文档

### Task 1: AiChatMessageService 扩展查询

**Files:**
- Modify: `AiChatMessageService.java`, `AiChatMessageServiceImpl.java`
- Modify: `AiChatMessagePersistenceService.java`

- [x] 新增 `listLatestUserAttachmentUrls(Long conversationId)`

### Task 2: LegalAiChatFacade

**Files:**
- Create: `LegalAiChatFacade.java`, `LegalAiChatFacadeImpl.java`
- Modify: `LegalOrchestrationAttachmentService.java`

- [x] Facade 委托 Service
- [x] AttachmentService 移除 Mapper 依赖

### Task 3: 单测 + 配置文档

- [x] `LegalOrchestrationAttachmentServiceTest`
- [x] `laby-project/reference.md` 配置索引表
- [x] `mvn -pl laby-module-legal,laby-module-ai test`

---

## Wave 2 — BPM 非阻塞

- [x] BPMN：`aiRound2Enqueue` + `awaitAiRound2` ReceiveTask
- [x] `LegalContractBpmAuditSignalService` + `LegalAiAuditReceiveListener`
- [x] `LegalAiAuditDelegate` 仅 enqueue；Consumer 完成后 signal
- [x] 删除 `LegalAuditCompletionWaiter`
- [x] 单测：`LegalContractBpmAuditSignalServiceTest`、`LegalAiAuditDelegateTest`
- [x] `mvn -pl laby-module-legal test`
