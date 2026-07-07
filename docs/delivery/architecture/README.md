# laby-ai-legal 架构图 · Next-AI-DrawIO

使用 [Next-AI-DrawIO](https://github.com/DayuanJiang/next-ai-draw-io) 生成可编辑 draw.io 架构图（自然语言 → XML）。

## 交付预览（PNG）

与 [交付索引](../README.md#架构图) 同步，本目录上一级已存放导出的预览图：

| 图 | 文件 |
|----|------|
| 系统架构 | [../系统架构.png](../系统架构.png) |
| 技术架构 | [../技术架构.png](../技术架构.png) |

## 源文件与工具

| 文件 | 说明 |
|------|------|
| [laby-admin-drawio-prompts.md](./laby-admin-drawio-prompts.md) | 系统 / 技术 / legal 模块提示词（手绘风格） |
| [laby-admin-architecture.drawio](./laby-admin-architecture.drawio) | **双页源文件**（系统架构 + 技术架构 N-Tier） |
| [generate-sketch-diagram.py](./generate-sketch-diagram.py) | 重新生成手绘布局（无动画、宽松边距） |

更新 PNG：在 draw.io 或 MCP 中打开 `.drawio` → 分别导出两页 → 覆盖 `docs/delivery/系统架构.png`、`技术架构.png`。

## 使用方式

| 方式 | 说明 |
|------|------|
| **在线** | [next-ai-drawio.jiang.jp](https://next-ai-drawio.jiang.jp/) — 粘贴提示词即可 |
| **Cursor MCP** | `drawio` → `start_session` → 浏览器 `localhost:6002` 实时预览 |

### MCP 配置（`.cursor/mcp.json`）

```json
"drawio": {
  "command": "npx",
  "args": ["-y", "@next-ai-drawio/mcp-server@latest"]
}
```

1. 完全退出并重启 Cursor
2. Settings → MCP → 确认 **drawio** 绿点 Connected
3. 新开 Agent 对话，读取 [laby-admin-drawio-prompts.md](./laby-admin-drawio-prompts.md) 生成架构图

重新生成布局：

```bash
python docs/delivery/architecture/generate-sketch-diagram.py
```

输出 [laby-admin-architecture.drawio](./laby-admin-architecture.drawio)；在 MCP 预览后导出 PNG 到上级目录。

官方文档：[packages/mcp-server/README](https://github.com/DayuanJiang/next-ai-draw-io/tree/main/packages/mcp-server)

## 项目内参考

- [法务架构设计 §3](../2026-06-03-legal-contract-architecture-design.md)
