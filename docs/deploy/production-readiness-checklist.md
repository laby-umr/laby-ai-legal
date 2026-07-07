# 生产就绪检查清单（AI / 法务）

适用于本次生产加固（Redis Session、审核 MQ、限流、SSE 等）上线前自检。

**一键脚本（Windows）：** `powershell -File docs/deploy/verify-production-readiness.ps1`

## 1. 基础设施

| 检查项 | 命令 / 方式 | 期望 |
|--------|-------------|------|
| Redis 版本 ≥ 5.0 | `redis-cli INFO server \| grep redis_version` | 5.x / 6.x / 7.x |
| Redis 连通 | 应用启动日志无 Redis 连接失败 | 正常 |
| MySQL 连接池 | `application-*.yaml` → `max-active: 80` | 已配置 |
| JVM 堆 | Docker `JAVA_OPTS` 或本地 `-Xmx2g` | ≥ 2GB |

## 2. 关键配置

确认 `application.yaml`（或对应 profile）：

```yaml
laby:
  ai:
    agentscope:
      session-store: redis   # 必须为 redis，多实例才可扩
  legal:
    audit:
      max-concurrent-per-tenant: 5

spring:
  servlet:
    multipart:
      max-file-size: 30MB
      max-request-size: 35MB
  task:
    execution:
      pool:
        core-size: 8
        max-size: 32
        queue-capacity: 100
```

## 3. 启动后冒烟

### 3.1 Redis Session 锁

1. 打开合同详情 → Agent 问答，连续快速发两条消息。
2. 第二条应提示「Agent 正在处理」或编排对话「当前对话正在处理上一条消息」。

### 3.2 审核进度（多实例）

1. 新建合同并触发 AI 审核。
2. 轮询 `GET /legal/contract/audit-progress?contractId=xxx`。
3. 应看到 `RUNNING` → `COMPLETED`；重启应用后进行中任务进度仍可查（Redis）。

### 3.3 审核 MQ

1. 创建合同或触发二轮 `auditAsync`。
2. Redis 中应存在 Stream：`LegalContractAuditMessage`。
3. 日志关键字：`审核任务已入队`、`LegalContractAuditConsumer`。

```bash
redis-cli XINFO STREAM LegalContractAuditMessage
redis-cli XINFO GROUPS LegalContractAuditMessage
```

### 3.4 SSE 租户头

1. 开启多租户（`VITE_APP_TENANT_ENABLE=true`）。
2. 浏览器 DevTools → Network → `send-stream` / `chat-stream` 请求头。
3. 应包含 `tenant-id`（及管理员代访时的 `visit-tenant-id`）。

### 3.5 上传限制

1. 上传 **29MB** docx → 成功。
2. 上传 **31MB** docx → 业务提示「不能超过 30MB」（非裸 413）。

### 3.6 限流

1. 同一用户 60 秒内连续发起 **11 次** AI 对话流式请求。
2. 应返回 429 /「请求过于频繁」类提示。

## 4. 反向代理（生产必做）

将 `docs/deploy/nginx-admin-api-sse.conf` 合并到网关，重点：

- `proxy_read_timeout 300s`
- `proxy_buffering off`
- `client_max_body_size 35m`

## 5. 多实例部署

| 能力 | 依赖 |
|------|------|
| Agent 会话锁 | `session-store: redis` |
| 编排对话互斥 | 同上（`chat:conv:{id}` 锁） |
| 审核进度轮询 | `LegalAiAuditProgressService` → Redis |
| 审核任务队列 | Redis Stream + Consumer Group `laby-server` |
| 租户审核并发 | `laby:legal:audit:running:tenant:{id}` |

**不要**在多 Pod 下使用 `session-store: workspace`。

## 6. 监控建议

- Druid 活跃连接数（告警 > 70）
- JVM 堆使用率（告警 > 85%）
- Redis 内存与 Stream 积压（`XLEN LegalContractAuditMessage`）
- LLM API 错误率 / 429
- SSE 断开率（网关 499/502）

## 7. 回滚

若 Redis Session 异常，可临时回滚：

```yaml
laby.ai.agentscope.session-store: workspace
```

**仅适用于单实例**；多实例不可回滚到此模式。

审核 MQ 消费异常时，流水线内 `auditForPipeline` 仍为同步路径，新建合同主流程不受影响；仅 `auditAsync`（如二轮）依赖队列。
