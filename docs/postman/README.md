# Postman — Laby Admin

## 文件

| 文件 | 说明 |
|------|------|
| `laby-admin.postman_collection.json` | 主 Collection（随 API 变更提交 Git） |
| `laby-admin.postman_environment.local.json` | 本地环境变量模板 |

## 本地 Import（API 变更后必做）

1. Postman → **Import** → 选择本目录 `laby-admin.postman_collection.json`
2. **Import** → `laby-admin.postman_environment.local.json`
3. 右上角环境选 **Laby Admin Local**
4. 修改变量（如需要）：
   - `baseUrl` → `http://localhost:48080`
   - `tenantId` → `1`
5. 运行 **`_Auth / Login`** 获取 `accessToken`（需本地 `captcha.enable=false`）
6. 运行本需求相关请求

## 重新 Import

Postman **不会**自动同步 Git。Collection 更新后需 **再次 Import**（可选 Merge 或 Replace）。

## 规范

- 工作流：`.cursor/skills/laby-workflow/SKILL.md` §Postman
- MCP 路由：`.cursor/rules/15-mcp-postman.mdc`
- 需配置环境变量 `POSTMAN_API_KEY`（Postman → Settings → API Keys）
