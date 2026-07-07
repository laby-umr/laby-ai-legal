# laby-module-legal

法务合同审核业务模块（BPM + AI）。

## 初始化

1. 全新库按顺序执行：`ruoyi-vue-pro.sql` → `quartz.sql`（可选）→ **`sql/mysql/laby-init.sql`**（法务 + AI 知识库增量，含菜单与权限）
2. 流程 Key：`legal_contract_review`  
   - BPMN 源文件：`src/main/resources/processes/legal_contract_review.bpmn20.xml`（**开始 → 法务处置意见**，不含「解析合同 / AI 首轮」节点）  
   - **启动应用时会自动检测并部署**（`LegalContractBpmAutoDeployRunner`），若库里仍是旧设计器模型会自动升级一版。  
   - 日志关键字：`[LegalContractBpmAutoDeploy] 已部署`  
   - **已发起的旧流程实例**仍绑定旧版定义，流程图会继续出现「开始无线 / 解析合同」；请**新建合同**验证。  
   - 也可在 **工作流 → 流程模型** 手动导入同一 BPMN 发布。
3. 模型元数据（自定义表单，类型=业务表单）见 `sql/mysql/legal_bpm_seed.sql`（仅说明，无 SQL）：
   - 创建合同：在「我的合同」列表点击「新建合同审核」弹窗完成（无需单独菜单页）
   - `formCustomViewPath`：`/legal/contract/review.vue`（流程详情嵌入，传入 `id`=合同编号）
4. 权限：`legal:contract:create`、`legal:contract:query`、`legal:contract:update`

## 已实现能力（Sprint 1–2）

| 能力 | 说明 |
|------|------|
| 合同 CRUD / 分页 | `LegalContractController` |
| Word 上传 | `POST /legal/contract/upload` → 基础设施文件表 |
| docx 解析 | POI → `legal_contract_paragraph` |
| AI 审核 | `AiModelService` + JSON 意见 + Markdown 报告 |
| 意见处置 | 采纳/忽略/撤销、批量、人工补充 |
| 二轮 AI | `complete-opinion` + BPM 变量 `needSecondRound` |
| 段落列表 | `GET /legal/contract/list-paragraph` |
| Word 导出 | `POST /legal/contract/export-report`；BPM `legalExportDelegate` 自动导出 |
| BPM 委托 | 解析 / AI / 导出归档 |

## 前端（web-ele）

- 列表：`/legal/contract/list`（含「新建合同审核」弹窗，支持 `.doc` / `.docx`）
- 审核 / 详情：均为 `review.vue`（`review?id=` 可编辑；`detail?id=` 只读详情；BPM 嵌入 `review.vue` + `props.id`）

## 生产部署（AI 并发加固）

多实例 / 正式环境上线前请阅读：**`docs/deploy/production-readiness-checklist.md`**

要点：

- `laby.ai.agentscope.session-store` 必须为 **`redis`**
- 审核进度存 Redis（`LegalAiAuditProgressService`），异步审核走 Redis Stream（`LegalContractAuditMessage`）
- 租户并行审核上限：`laby.legal.audit.max-concurrent-per-tenant`（默认 5）
- 网关 SSE：`docs/deploy/nginx-admin-api-sse.conf`

## 降级说明

- 流程未部署时，创建合同**无 BPM**，仍会异步解析；AI 需配置默认对话模型才有真实输出。
- `.doc` 与 PDF 不在 V1 范围。

## SQL

业务增量已合并为 **`sql/mysql/laby-init.sql`**（可重复执行）。历史分散脚本已移除，详见 `docs/delivery/README.md`。

## 包结构（对齐 ruoyi-vue-pro / demo01）

与 URL、表名一一对应，**不再使用 `rule` 聚合包**：

| API 前缀 | Java 包（controller / service / dal） |
|----------|----------------------------------------|
| `/legal/contract` | `…contract` |
| `/legal/contract-type` | `…contracttype` |
| `/legal/standard-clause` | `…standardclause` |
| `/legal/audit-rule` | `…auditrule`（含 `LegalAuditContextService`） |
| 审核报告构建 | `…service.report`（`LegalAuditReportBuilder`） |
| 意见 | `…opinion` |

前端页面：`views/legal/contract-type`、`standard-clause`、`audit-rule`（菜单 component 同路径；路由仍可在「规则配置」目录下）。

## 待办（Sprint 3+ / Phase C–D）

| 优先级 | 项 | 状态 |
|--------|-----|------|
| P1 | 意见撤销 | ✅ 前后端 |
| P1 | 业务状态时间线 + BPM 流程入口 | ✅ 审核页「基本信息」 |
| P1 | Word 导出后自动下载 | ✅ |
| P1 | 六章审核报告 | ✅ `LegalAuditReportBuilder` |
| P2 | AI 流式 / reasoning 展示 | ✅ 审核中轮询 `/audit-progress` |
| P1 | 合同原文问答（流式 + 三档模式） | ✅ |
| P1 | 段落「不需 AI 审核」标记 | ✅（含于 `laby-init.sql`） |
| P2 | 全局规则库 + RAG 引用标准条款 | ✅ Phase C（含于 `laby-init.sql`） |
| P3 | 原合同 docx 批注回写（WPS 级修订） | 未做 |
| P3 | 飞书 SSO、组织同步 | 未做 |
