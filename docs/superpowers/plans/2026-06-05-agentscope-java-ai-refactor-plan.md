# AgentScope 2 Java AI 重构 — 实施计划（Phase 0～2 可执行）

**依据 Spec：** [2026-06-05-agentscope-java-ai-refactor-spec.md](../specs/2026-06-05-agentscope-java-ai-refactor-spec.md)

---

## Phase 0：运行时基座（2～3 周）

### 交付物

- [ ] Maven 模块 `laby-module-ai-runtime-api`
- [ ] Maven 模块 `laby-module-ai-runtime-agentscope`
- [ ] Maven 模块 `laby-module-ai-runtime-springai`（过渡）
- [ ] `AiRuntimeFacade` 接口 + `AiRuntimeContext`
- [ ] `AgentScopeModelAdapter`（OpenAI 兼容 / 通义 / DeepSeek）
- [ ] 配置 `laby.ai.runtime` + 单测

### 任务分解

| # | 任务 | 文件/位置 |
|---|------|-----------|
| 0.1 | 父 POM 引入 `agentscope-harness` BOM/版本 | `laby-module-ai-runtime-agentscope/pom.xml` |
| 0.2 | 定义 `AiRuntimeFacade` | `.../runtime/api/AiRuntimeFacade.java` |
| 0.3 | 定义 Event DTO（对齐现有 SSE） | `.../runtime/api/event/*` |
| 0.4 | `AgentScopeModelAdapter` 读 `ai_model`/`ai_api_key` | `.../agentscope/model/*` |
| 0.5 | Spring AI 过渡实现 `SpringAiRuntimeFacade` | `.../springai/*` |
| 0.6 | `@Configuration` 按 `laby.ai.runtime` 选择 Bean | `laby-server` 或 ai 模块 config |
| 0.7 | 连通性测试（需 API Key） | `AgentScopeModelAdapterTest` |

### 验收命令

```bash
mvn -pl laby-module-ai-runtime-api,laby-module-ai-runtime-agentscope,laby-module-ai-runtime-springai test
```

---

## Phase 1：法务 Agent（3～4 周）

### 交付物

- [ ] 8 个 Tool 的 AgentScope 版本
- [ ] `LegalContractAgentServiceImpl` 重写（或 `LegalContractAgentServiceImplV2` 并行）
- [ ] Middleware：Tenant / Trace / SSE / Proposal
- [ ] Feature flag：`legal.agent.runtime=agentscope`

### Tool 迁移顺序

1. `legal_get_contract_meta`（最简单）
2. `legal_search_paragraphs`
3. `legal_get_audit_opinions`
4. `legal_get_audit_report`
5. `legal_search_knowledge`
6. `legal_compare_audit_rounds`
7. `legal_propose_adopt_opinion`
8. `legal_propose_skip_paragraph`

### 关键类

| 现类 | 新类/改法 |
|------|-----------|
| `LegalAgentToolProvider` | `LegalAgentScopeToolRegistry` |
| `LegalAgentToolAspect` | `LegalAgentTraceMiddleware` |
| `LegalAgentSseEventHolder` | `AgentScopeSseBridge` |
| `LegalContractAgentServiceImpl` | 注入 `AiRuntimeFacade.runAgent()` |

### 验收

- [ ] 合同问答 Agent 模式 E2E
- [ ] 提案 Confirm 流程
- [ ] `legal_agent_step_log` 有记录
- [ ] 与 Spring AI 版 A/B 对比 10 条典型问题

---

## Phase 2：审核 Pipeline（2～3 周）

### 交付物

- [ ] `BatchCompletionRunner`（非 ReAct）
- [ ] `LegalAiAuditPipelineService` 改用 runtime
- [ ] reasoning → `LegalAiAuditProgressHolder`
- [ ] BPM 二轮回归

### 不改

- `LegalDeterministicAuditEngine`
- `LegalAiOrchestratorImpl` 对外接口
- `LegalAiAuditDelegate`

### 验收

```bash
mvn -pl laby-module-legal test "-Dtest=LegalPlaybookEvalRunnerTest"
# + 人工：创建合同 → 首轮 AI → 意见列表非空
```

---

## Phase 3～5 预览

| Phase | 内容 | 周期 |
|-------|------|------|
| 3 | 法务 Chat 普通模式 + SkillPack | 1～2 周 |
| 4 | `AiChatMessageServiceImpl` + RAG | 4～6 周 |
| 5 | 移除 Spring AI 依赖 | 1～2 周 |

---

## Git 分支策略

```
main
 └── feature/agentscope-poc          # Phase 0 POC
      └── feature/agentscope-legal-agent   # Phase 1
           └── feature/agentscope-audit    # Phase 2
```

每 Phase 独立 PR，可合并到 main 但默认 flag 关闭。

---

## 配置清单（运维）

```yaml
laby:
  ai:
    runtime: spring-ai          # Phase 5 改为 agentscope
    agentscope:
      session-store: redis
      session-key-prefix: "as:"
    scenes:
      legal-agent:
        runtime: agentscope     # Phase 1 起可开
      legal-audit:
        runtime: spring-ai      # Phase 2 完成后改 agentscope
```

Redis 无新增实例，仅 key 前缀隔离。
