# Agent 记忆架构 P0～P2 Implementation Plan

> **Status:** 已完成（2026-06-09），待统一联调测试。

**Goal:** 消除双轨记忆、按 sessionId 隔离法务多 Tab、启用 Compaction 与 token 预算裁剪，并补齐 Redis 编排上下文与 Session 增量 flush。

**Architecture:** Harness 路径 Session 优先（仅本轮增量）；纯 LLM 路径 DB + `TokenBudgetAgentMemoryPolicy`；法务 sessionId 贯穿 Mapper/Service/API；P1 编排上下文与 Session flush 走 Redis。

**Tech Stack:** Java 17, Spring Boot, AgentScope Harness 2.0 RC1, jtokkit, Redis, Vue3 Element Plus

**Spec:** [`docs/superpowers/specs/2026-06-09-agent-memory-p0-p2-spec.md`](../specs/2026-06-09-agent-memory-p0-p2-spec.md)

---

### Task 1: 配置与 Compaction（P0-1）

- [x] 设置 `compaction-token-threshold: 120000`
- [x] 新增 `history-token-budget: 32000`
- [x] `compaction-summary-persist: true`（默认开启，便于联调）

### Task 2: TokenBudgetAgentMemoryPolicy（P0-5 / P1-1）

- [x] 实现从尾部向前累加 token 的 trim
- [x] 单测：超预算时丢弃最早轮次

### Task 3: AI Chat Session 优先（P0-2）

- [x] `roleHasTools` 时 `includeHistory=false`
- [x] 纯 LLM 路径 `filterContextMessages` 后接 token trim
- [x] 纯 LLM 上下文显式跳过 `type=summary`

### Task 4: 法务 sessionId + history（P0-3 / P0-4）

- [x] Mapper/Service 增加 sessionId 参数
- [x] Agent `buildAgentMessages` 仅当前 user；Agent 路径不再查 DB history
- [x] HistoryHelper 使用 properties + token policy；跳过 summary 行
- [x] 删除未使用的 `appendAgentScopeHistory`

### Task 5: 前端 sessionId 列表（P0-6）

- [x] API 增加可选 `sessionId` query
- [x] 加载消息时传 `ensureChatSessionId()`

### Task 6: Redis 编排上下文（P1-3）

- [x] Holder 委托 Store；Redis 不可用时 InMemory

### Task 7: Session 增量 flush（P1-4）

- [x] dirty 文件集合；save 仅 flush dirty

### Task 8: P2 情节记忆 + Checkpoint

- [x] `legal_contract_memory` 表 + CRUD + Agent 附录
- [x] Checkpoint 存取 + 前端自动/手动恢复
- [x] SQL：`sql/mysql/laby-legal-agent-memory-p2.sql`

### Task 9: 验证

- [x] 单元测试（memory / checkpoint / compaction / sessionId）
- [ ] 统一联调：`mvn -pl laby-module-ai,laby-module-legal -am test`
- [ ] 手工：多 Tab sessionId、Compaction 摘要、编排恢复、记忆 CRUD

### Task 10: 扩展（本轮补充）

- [x] 法务 Agent Compaction 摘要落库 + 审阅页展示
- [x] 情节记忆手动新增/编辑/删除 + 「全部会话」切换
- [x] LLM 抽取支持 `[fact|risk|decision|milestone]` 类型标签
