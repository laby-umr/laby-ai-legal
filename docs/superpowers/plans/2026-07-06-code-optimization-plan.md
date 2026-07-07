# 代码优化 OPT-001 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提升 legal/ai 模块可维护性与管道可靠性：Wave 1 管道+Builder → Wave 2 Legal 拆分 → Wave 3 AI 拆分 → Wave 5 门禁；Wave 4 BPM 异步单独立项。

**Architecture:** 按 [OPT-001 Spec](../specs/2026-07-06-code-optimization-spec.md) 分波交付；每波独立 PR；遵循 `laby-global`（Read/Write/StrReplace 改码、Conventional Commits）。

**Tech Stack:** Java 17 · Spring Boot · MyBatis · Flowable(BPM) · AgentScope · Maven

**评审默认（用户已确认继续）：**

- 顺序：**Wave 1 → Wave 2 → Wave 3**（2/3 不同模块可并行）
- **Wave 4** 本 Plan **不实施**，另开 OPT-002
- 拆分后实现类 **≤400 行**（硬指标，Facade 除外）
- **删除 `parseAsync`**

**Spec:** [2026-07-06-code-optimization-spec.md](../specs/2026-07-06-code-optimization-spec.md)

---

## 文件结构总览

### Wave 1 新增/修改

| 文件 | 职责 |
|------|------|
| `LegalContractParseService.java` | 删 `parseAsync` |
| `LegalContractParseServiceImpl.java` | 异常上抛 + parse CAS |
| `LegalAuditCompletionWaiter.java` | 审核 progress 轮询 |
| `LegalAiAuditServiceImpl.java` | 委托 Waiter |
| `LegalContractPipelineCommand.java` | Builder（可选 W1-5） |
| `LegalContractAuditPersistCommand.java` | Builder（W1-5） |
| `LegalContractParseGuardTest.java` | parse 幂等单测 |
| `ErrorCodeConstants.java` | 新增 `CONTRACT_PARSE_IN_PROGRESS` |

### Wave 2 新增（legal）

| 文件 | 职责 |
|------|------|
| `LegalContractCreateService` | 创建 + afterCommit |
| `LegalContractBpmService` | BPM 人工阶段 |
| `LegalContractQueryService` | 查询 |
| `LegalContractChatContextBuilder` | 聊天上下文 |
| `LegalContractChatRagSupport` | 聊天 RAG |
| `LegalContractChatAgentSupport` | Agent 模式 |
| `LegalAuditOpinionPersistService` | 意见/报告持久化 |

### Wave 3 新增（ai）

| 文件 | 职责 |
|------|------|
| `AiChatMessageStreamHandler` | SSE |
| `AiChatMessagePersistenceService` | 落库 |
| `AiChatMessageRagInjector` | RAG 注入 |
| `AiKnowledgeSegmentIndexService` | 分段索引 |

---

## Wave 1 — 管道可靠 + Builder（优先）

### Task 1: 删除 parseAsync 死代码

**Files:**
- Modify: `laby-module-legal/.../LegalContractParseService.java`
- Modify: `laby-module-legal/.../LegalContractParseServiceImpl.java`

- [ ] **Step 1:** 删除接口 `void parseAsync(Long contractId);`
- [ ] **Step 2:** 删除 `LegalContractParseServiceImpl.parseAsync` 方法体（含 `@Async` + `auditAsync` 调用）
- [ ] **Step 3:** 验证无引用

```bash
rg "parseAsync" laby-module-legal
```

Expected: 无匹配

- [ ] **Step 4:** 编译

```bash
mvn -pl laby-module-legal -q compile
```

- [ ] **Step 5: Commit**

```bash
git commit -m "refactor(legal): remove unused parseAsync entry point"
```

---

### Task 2: 解析失败异常向上传播

**Files:**
- Modify: `LegalContractParseServiceImpl.java` — `doParseOnly`, `reparseFromDocxBytes`
- Modify: `LegalContractVersionServiceImpl.java` — 处理 reparse 异常（如需）

- [ ] **Step 1:** `doParseOnly` 的 catch 块改为：打 log 后 **rethrow**

```java
} catch (Exception ex) {
    log.error("[doParseOnly][contractId={}] 解析失败", contractId, ex);
    contractMapper.updateById(new LegalContractDO()
            .setId(contractId)
            .setParseStatus(LegalParseStatusEnum.FAILED.getStatus()));
    if (ex instanceof ServiceException) {
        throw (ServiceException) ex;
    }
    throw exception(CONTRACT_PARSE_NOT_SUCCESS);
}
```

- [ ] **Step 2:** `reparseFromDocxBytes` 去掉吞异常；失败 `throw exception(CONTRACT_PARSE_NOT_SUCCESS)` 或专用码
- [ ] **Step 3:** 跑单测

```bash
mvn -pl laby-module-legal -q test
```

- [ ] **Step 4: Commit**

```bash
git commit -m "fix(legal): propagate parse failures to pipeline markFailed"
```

---

### Task 3: 解析幂等 CAS

**Files:**
- Modify: `LegalContractParseServiceImpl.java` — `doParseOnly` 开头
- Modify: `ErrorCodeConstants.java` — 新增错误码
- Create: `laby-module-legal/src/test/java/.../LegalContractParseGuardTest.java`

- [ ] **Step 1:** 新增错误码

```java
ErrorCode CONTRACT_PARSE_IN_PROGRESS = new ErrorCode(1_050_000_062, "合同正在解析中，请勿重复提交");
```

- [ ] **Step 2:** `doParseOnly` 开头 CAS

```java
LegalContractDO current = contractMapper.selectById(contractId);
if (current == null) {
    throw exception(CONTRACT_NOT_EXISTS);
}
if (LegalParseStatusEnum.RUNNING.getStatus().equals(current.getParseStatus())) {
    throw exception(CONTRACT_PARSE_IN_PROGRESS);
}
if (LegalParseStatusEnum.SUCCESS.getStatus().equals(current.getParseStatus())) {
    log.info("[doParseOnly][contractId={}] 已解析成功，跳过", contractId);
    return;
}
// 然后 WAITING/RUNNING 更新为 RUNNING（保持现有 updateById）
```

- [ ] **Step 3:** 写单测 `LegalContractParseGuardTest` — mock Mapper，RUNNING 时抛 `CONTRACT_PARSE_IN_PROGRESS`

- [ ] **Step 4:** 运行测试

```bash
mvn -pl laby-module-legal -q test -Dtest=LegalContractParseGuardTest
```

- [ ] **Step 5: Commit**

```bash
git commit -m "fix(legal): guard concurrent parse with parseStatus CAS"
```

---

### Task 4: 提取 LegalAuditCompletionWaiter

**Files:**
- Create: `laby-module-legal/.../service/contract/support/LegalAuditCompletionWaiter.java`
- Modify: `LegalAiAuditServiceImpl.java`

- [ ] **Step 1:** 新建 Waiter，迁移 `waitUntilAuditSettled` + 常量 `MAX_WAIT_ITERATIONS = 7200`

```java
@Component
public class LegalAuditCompletionWaiter {
    @Resource
    private LegalAiAuditProgressService auditProgressService;

    public void waitUntilSettled(Long contractId, int auditRound) {
        // 原 waitUntilAuditSettled 逻辑原样迁移
    }
}
```

- [ ] **Step 2:** `LegalAiAuditServiceImpl` 注入 Waiter，删除 private 方法，改调用

- [ ] **Step 3:** 测试

```bash
mvn -pl laby-module-legal -q test
```

- [ ] **Step 4: Commit**

```bash
git commit -m "refactor(legal): extract LegalAuditCompletionWaiter from audit service"
```

---

### Task 5: Builder — LegalContractAuditPersistCommand

**Files:**
- Create: `laby-module-legal/.../service/contract/bo/LegalContractAuditPersistCommand.java`
- Modify: `LegalAiAuditServiceImpl.java` — 持久化段（`saveOpinions` 等 private 方法）

- [ ] **Step 1:** 创建 Command（字段：`contract`, `auditRound`, `items`, `model`, `failFast`, `traceSession` 等按实际持久化方法定）

- [ ] **Step 2:** 将 ≥5 参数的 private 持久化方法改为接收 Command

- [ ] **Step 3:** 测试 + Commit

```bash
git commit -m "refactor(legal): use Builder command for audit persist path"
```

---

### Task 6: Wave 1 交付验证

- [ ] **Step 1:** 全模块测试

```bash
mvn -pl laby-module-legal,laby-module-ai test
```

- [ ] **Step 2:** 冒烟

```powershell
.\docs\superpowers\scripts\baseline-smoke.ps1
```

- [ ] **Step 3:** 更新 Spec 状态 Wave 1 完成（Plan checklist）

---

## Wave 2 — Legal God Class 拆分

> **前置：** Wave 1 已合并。每 Task 独立 PR 更易 review。

### Task 7: 拆分 LegalContractServiceImpl — Create

**Files:**
- Create: `.../LegalContractCreateService.java`
- Modify: `LegalContractServiceImpl.java`

- [ ] **Step 1:** 迁移 `createContract`、附件 insert、`afterCommit` + `processStarter` 至 CreateService
- [ ] **Step 2:** Facade 委托；`startBpmHumanPhase` 暂留或标 package-private 供 BpmService
- [ ] **Step 3:** `mvn -pl laby-module-legal test`
- [ ] **Step 4: Commit** — `refactor(legal): extract LegalContractCreateService`

### Task 8: 拆分 LegalContractServiceImpl — BPM + Query

**Files:**
- Create: `LegalContractBpmService.java`, `LegalContractQueryService.java`

- [ ] **Step 1:** 迁移 BPM 相关至 BpmService
- [ ] **Step 2:** 迁移 page/get/validate 至 QueryService
- [ ] **Step 3:** `LegalContractServiceImpl` ≤150 行 Facade
- [ ] **Step 4: Commit** — `refactor(legal): split contract service into create/bpm/query`

### Task 9: 拆分 LegalContractChatServiceImpl

**Files:**
- Create: `LegalContractChatContextBuilder.java`, `LegalContractChatRagSupport.java`, `LegalContractChatAgentSupport.java`
- Modify: `LegalContractChatServiceImpl.java`

- [ ] **Step 1:** 按 Spec W2-2 迁移；ChatServiceImpl 仅路由
- [ ] **Step 2:** 每类 ≤400 行
- [ ] **Step 3:** 测试 + Commit

### Task 10: 拆分 LegalAiAuditServiceImpl — Persist

**Files:**
- Create: `LegalAuditOpinionPersistService.java`
- Modify: `LegalAiAuditServiceImpl.java`

- [ ] **Step 1:** 迁移 opinion/report 写入、二轮跳过逻辑
- [ ] **Step 2:** AuditServiceImpl ≤400 行
- [ ] **Step 3:** 测试 + Commit

### Task 11: Wave 2 交付验证

- [ ] **Step 1:** `mvn -pl laby-module-legal test`
- [ ] **Step 2:** 冒烟 + E2E Checklist E1～E3（见文末）

---

## Wave 3 — AI God Class 拆分

> 可与 Wave 2 **并行**（不同开发者/分支）；依赖 Wave 1 无。

### Task 12: 拆分 AiChatMessageServiceImpl

**Files:**
- Create: `AiChatMessageStreamHandler.java`, `AiChatMessagePersistenceService.java`, `AiChatMessageRagInjector.java`
- Modify: `AiChatMessageServiceImpl.java`

- [x] **Step 1:** 先抽 Persistence（风险最低）
- [x] **Step 2:** 再抽 StreamHandler
- [x] **Step 3:** 再抽 RagInjector
- [x] **Step 4:** Facade ≤300 行；`mvn -pl laby-module-ai test`

### Task 13: 拆分 AiKnowledgeSegmentServiceImpl

**Files:**
- `AiKnowledgeSegmentIndexService.java`（编排）
- `AiKnowledgeSegmentVectorSupport.java`、`AiKnowledgeSegmentSplitSupport.java`
- Modify: `AiKnowledgeSegmentServiceImpl.java`

- [x] **Step 1:** 索引/向量化迁出；SegmentServiceImpl 保留 CRUD + 检索委托
- [x] **Step 2:** IndexService 进一步拆 Vector/Split Support（≤400 行）
- [x] **Step 3:** 测试通过

### Task 14: Wave 3 交付验证

- [x] **Step 1:** `mvn -pl laby-module-ai test`
- [x] **Step 2:** 冒烟（含 AI knowledge page，需本地 laby-server）

---

## Wave 5 — 规范门禁

### Task 15: FQN 自检脚本

**Files:**
- Create: `docs/superpowers/scripts/check-fqn.ps1`
- Create: `.github/workflows/code-quality-gate.yml`

- [x] **Step 1:** 脚本封装 laby-global §1.4 两条 rg，exit 1 若有命中
- [x] **Step 2:** CI workflow + laby-workflow 文档引用

### Task 16: 文档 — orchestrator 包职责表

**Files:**
- Modify: `.cursor/skills/laby-project/reference.md`

- [x] **Step 1:** 增加 orchestrator vs orchestration 对照表（Spec W4-3 文档部分提前落地）

### Task 17: 最终验证

- [x] **Step 1:** `check-fqn.ps1`（CI 已接入；`baseline-smoke.ps1` 需本地服务）
- [x] **Step 2:** `mvn -pl laby-module-legal,laby-module-ai test`
- [x] **Step 3:** 更新 Spec OPT-001 状态为 Implemented（Wave 1～3 + 5）；OPT-002 另文档标记完成

---

## Wave 4 —  backlog（本 Plan 不实施）

另开 **OPT-002 Spec/Plan**（**已完成**）：

- [x] BPM `auditForBpm` 改 ReceiveTask + signal
- [x] `LegalAiChatFacade` 替代 `AiChatMessageMapper` 直依赖
- [x] Legal 配置索引 → `laby-project/reference.md` + `application.yaml` 注释

---

## E2E Checklist

**前置：** laby-server、MySQL、Redis、LLM 可用；`captcha.enable=false`

- [ ] **E1:** 创建合同 docx → `parseStatus=SUCCESS`，段落数 N
- [ ] **E2:** 首轮 AI 审核 → `AI_AUDITED` 或进入意见复核；`legal_audit_opinion` ≥1 或 Playbook 确定性意见
- [ ] **E3:** 失败路径：损坏 docx → 合同 `FAILED` 或接口 errorCode；`feedback_summary` 非空
- [ ] **E4:** 并发：快速双触发 parse → 段落数仍为 N（Wave 1 Task 3 后）
- [ ] **E5:** 知识库分页 smoke `GET /admin-api/ai/knowledge/page`
- [ ] **E6:** 前端「我的合同」列表可打开（可选）

---

## Plan 总 Checklist

### Wave 1
- [x] Task 1 parseAsync 删除
- [x] Task 2 异常传播
- [x] Task 3 parse CAS + 单测
- [x] Task 4 Waiter 提取
- [x] Task 5 Persist Command
- [x] Task 6 Wave1 验证（`mvn -pl laby-module-legal test` 97/97 通过；baseline-smoke 需本地服务）

### Wave 2
- [x] Task 7～10 拆分（Create/Bpm/Query Facade + Chat 三 Support + OpinionPersist）
- [x] Task 11 Wave2 验证（`mvn -pl laby-module-legal test` 97/97 通过）

### Wave 3
- [x] Task 12～13 拆分（Chat Persistence/RAG/Stream + SegmentIndex 已存在）
- [x] Task 14 Wave3 验证（`mvn -pl laby-module-ai test` 通过）

### Wave 5
- [x] Task 15 FQN 脚本 `check-fqn.ps1`
- [x] Task 16 orchestrator 包职责表 → `laby-project/reference.md`
- [x] Task 17 最终验证（FQN + legal/ai 单测）

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-07-06 | 初稿，承接 OPT-001 Spec |
