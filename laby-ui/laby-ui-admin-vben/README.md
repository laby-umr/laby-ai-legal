# Laby Admin 前端（Element Plus）

本目录为精简后的 Vben Admin 单体前端，仅保留 **Element Plus** 应用：`apps/web-ele`。

## 启动

```bash
# 在 laby-ui-admin-vben 根目录
pnpm install
pnpm dev
```

默认访问：http://localhost:5777  
后端 API 代理见 `apps/web-ele/.env.development`（默认 `http://127.0.0.1:48080`）。

## 构建

```bash
pnpm build
# 或
pnpm build:ele
```

## 已移除

- Ant Design Vue / Naive UI / TDesign 等多套 UI 应用
- `docs`、`playground`、其他演示应用
- 商城、CRM、ERP 等业务前端页面（与后端精简保持一致）
