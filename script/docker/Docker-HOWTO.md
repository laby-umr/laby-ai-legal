# Docker 统一部署指南

所有本地依赖已合并到 **`script/docker/docker-compose.yml`** 一份配置中。

## 包含的服务

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | laby-mysql | 3306 | 业务库，默认 `laby-system` / `123456` |
| Redis | laby-redis | 6379 | 缓存 / Agent Session |
| Qdrant | laby-qdrant | 6333/6334 | AI 知识库向量库 |
| RabbitMQ | laby-rabbitmq | 5672 / **15672** | 消息队列（管理台 http://localhost:15672） |
| RocketMQ | laby-rocketmq-* | 9876 | 可选，`--profile rocketmq` 启动 |
| OnlyOffice | laby-onlyoffice | 8088 | 法务合同预览/审阅 |
| PDF 解析 | laby-parse-adapter | 8000 | 知识库 PDF 结构化解析（MinerU 适配层） |
| Docling 解析 | laby-docling-adapter | 8001 | DOC/DOCX/XLSX 等结构化解析（可选，见 `PARSE_DOCLING_URL`） |
| 后端 API | laby-server | 48080 | 可选，`--profile app` |
| 管理端 | laby-admin | 8080 | 可选，`--profile app` |

> OnlyOffice、PDF 解析等中间件均已合并到本目录 `docker-compose.yml`，不再维护 `docs/deploy/*-docker-compose.yml`。

## 快速启动

```bash
cd script/docker

# 1. 启动全部中间件（推荐日常开发）
docker compose --env-file docker.env up -d

# 2. 如需 RocketMQ
docker compose --env-file docker.env --profile rocketmq up -d

# 3. 如需 Docker 内运行后端+前端（需先 mvn package 构建 jar）
docker compose --env-file docker.env --profile app up -d
```

## 本地 IDEA 跑后端时的配置对照

后端在宿主机运行、中间件在 Docker 时，`application-local.yaml` 参考：

```yaml
spring:
  datasource: ... url: jdbc:mysql://127.0.0.1:3306/laby-system ...
  data.redis.host: 127.0.0.1
  rabbitmq.host: 127.0.0.1

rocketmq.name-server: 127.0.0.1:9876   # 若启用了 rocketmq profile

laby.ai.vector-store.qdrant.host: 127.0.0.1
laby.ai.vector-store.qdrant.port: 6334

laby.ai.document-parse.mineru.enabled: true
laby.ai.document-parse.mineru.base-url: http://127.0.0.1:8000
laby.ai.document-parse.docling.enabled: true
laby.ai.document-parse.docling.base-url: http://127.0.0.1:8001

laby.legal.onlyoffice.enabled: true
laby.legal.onlyoffice.document-server-url: http://127.0.0.1:8088/
laby.legal.onlyoffice.jwt-secret: laby-onlyoffice-dev-secret-change-me
```

## SQL 手动执行

全新库（非 Docker 自动初始化时）按顺序执行：

```
sql/mysql/ruoyi-vue-pro.sql
sql/mysql/quartz.sql          # 可选
sql/mysql/laby-init.sql       # 法务 + AI 知识库增量（可重复执行）
```

在 Navicat 中选中 `laby-system` 库后整段执行即可。

## 常用命令

```bash
# 查看状态
docker compose ps

# 查看日志
docker compose logs -f parse-adapter

# Docling 适配层（启用后）
# docker compose logs -f docling-adapter

# 停止
docker compose down

# 停止并删数据卷（慎用）
docker compose down -v
```
