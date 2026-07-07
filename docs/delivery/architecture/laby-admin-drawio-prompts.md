# laby-admin 架构图 · Next-AI-DrawIO 提示词

> 工具：[Next-AI-DrawIO](https://github.com/DayuanJiang/next-ai-draw-io)（自然语言 → draw.io 可编辑 XML）  
> 在线体验：[next-ai-drawio.jiang.jp](https://next-ai-drawio.jiang.jp/)  
> Cursor 集成：MCP `drawio`（见 [README](./README.md)）

---

## 方式 A：在线（最快，无需配置）

1. 打开 [next-ai-drawio.jiang.jp](https://next-ai-drawio.jiang.jp/)
2. 复制下方 **提示词 1 或 2** 粘贴到对话框
3. 生成后在 draw.io 面板微调 → File → Export as PNG/SVG

---

## 方式 B：Cursor MCP（推荐）

1. Settings → MCP → 确认 **drawio** 已 Connected（需重启 Cursor）
2. **新开 Agent 对话**，发送：

```
请用 drawio MCP：
1. 先调用 start_session 打开浏览器预览
2. 再读取 docs/delivery/architecture/laby-admin-drawio-prompts.md 中的「提示词 1」
3. 按提示词生成系统架构图
4. 完成后 export_diagram 保存到 docs/delivery/architecture/laby-admin-system.drawio
```

---

## 提示词 1 · 系统架构图（手绘风格）

```
请绘制 laby-admin 系统架构图（企业级法务合同 AI 审核平台），要求：

【风格 — 手绘 sketch】
- 全部形状启用 sketch=1 手绘线条（draw.io 手绘风格）
- 暖色纸面背景 #FFF9F0
- 圆角虚线分组框 + 柔和填充色（用户黄 / 平台蓝 / 模块紫 / legal 珊瑚 / 数据绿 / 外部橙）
- 连线用手绘曲线箭头，标注协议（HTTPS / REST/SSE）
- 禁止霓虹暗色、3D 立体、过多 emoji

【布局：从左到右三列】
列1「业务用户」：
  - 法务专员（合同审核 · AI 辅助）
  - 审批人（BPM 待办 · 流程审批）
  - 管理员（权限 · 系统配置）

列2「laby-admin 平台」大分组框内从上到下：
  - web-ele 前端（Vue3 · Vben · Element Plus）— 合同 / 知识库 / BPM / AI 对话
  - laby-server 接入层（Spring Boot 3.5 · REST · SSE · OAuth2 · 多租户）
  - 业务模块横排：legal★（Pipeline·Audit·Agent）、ai（RAG·AgentScope）、bpm、system、infra

列3「外部依赖」：
  - 数据：MySQL、Redis Stream、Qdrant、MinIO
  - 服务：LLM API、OnlyOffice、Flowable Engine

【连线】
  - 使用者 --HTTPS--> web-ele
  - web-ele --REST/SSE--> laby-server
  - laby-server --> legal（粉色线）
  - legal --> MySQL / Redis / MinIO
  - ai --> Qdrant / LLM
  - bpm --> Flowable
  - 底部标注：HTTPS → REST/SSE → legal → MySQL · Redis · MinIO
```

---

## 提示词 2 · 技术架构图（N-Tier · 手绘风格）

```
请绘制 laby-admin 技术架构 N-Tier 分层图，要求：

【风格 — 手绘 sketch】
- sketch=1 手绘线条 + 暖色纸面背景 #FFF9F0
- L5-L1 每层虚线泳道 + 左侧色条区分层级
- 主依赖链用粗手绘箭头居中向下
- 右侧 RAG Pipeline 用手绘虚线框
- 禁止暗色霓虹、线条交叉

【五层内容】
L5 表现层：web-ele（Vue3 · Vben · Element Plus）、Admin Pages
L4 接入层：laby-server（REST · SSE）、Security Starter（OAuth2 · Tenant）
L3 业务层：laby-module-legal★、laby-module-ai、laby-module-bpm、laby-module-system、laby-module-infra
L2 框架层：laby-framework（MyBatis-Plus）、AgentScope 2、Flowable Engine
L1 基础设施：MySQL、Redis、Qdrant、MinIO、LLM Provider、OnlyOffice

【主依赖链（粗箭头，居中）】
web-ele → laby-server → legal → laby-framework → MySQL

【次要关系】
右侧粉色虚线框「RAG Pipeline」：Dense(Qdrant) + Sparse(MySQL FULLTEXT) → RRF → Rerank · legal → Redis Stream
```

---

## 提示词 3 · 法务 legal 模块内部（可选）

```
请绘制 laby-module-legal 内部架构图：

前端 web-ele（合同工作台、Agent 面板）
  → laby-server Legal Controllers
  → legal 模块：
      Pipeline / Parse / Audit
      Opinion / Export / Version
      Chat + Agent Orchestration
      Agent Tools AOP
  → 依赖：ai（RAG/LLM）、bpm（Flowable）、infra（文件）
  → 存储：MySQL、Qdrant、MinIO

风格：draw.io 分层 + 模块框，箭头自上而下，legal 核心高亮。
```

---

## 导出路径建议

| 图 | 建议保存 |
|----|----------|
| 系统架构 | `docs/delivery/architecture/laby-admin-system.drawio` |
| 技术架构 | `docs/delivery/architecture/laby-admin-technical.drawio` |

`.drawio` 可用 [draw.io](https://app.diagrams.net/) 或 VS Code Draw.io 插件继续编辑。
