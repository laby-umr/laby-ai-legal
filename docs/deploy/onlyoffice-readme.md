# OnlyOffice 审阅工作台 — 官方集成方案

本文按 [ONLYOFFICE 官方插件文档](https://api.onlyoffice.com/docs/plugin-and-macros/tutorials/installing/onlyoffice-docs-on-premises/) 配置：**插件放在 Document Server 的 `sdkjs-plugins` 目录**，页面通过 **`serviceCommand` → `Gateway.internalcommand`** 与插件通信，插件内用官方 **`executeMethod('SearchNext')`** 定位正文。

> Nextcloud / OwnCloud 等开源集成也是「DS 打开文档 + editorConfig.plugins」，不在 API 域名单独挂插件。

---

## 架构（官方模式）

```
浏览器审阅页
  └─ DocsAPI.DocEditor(..., editorConfig.plugins.autostart + pluginsData)
        └─ OnlyOffice DS (8088)
              └─ sdkjs-plugins/legal-locate/   ← 系统 autostart 插件
                    ├─ config.json
                    ├─ index.html → vendor/plugins.js + locate-core.js
                    └─ translations/

定位流程：
  页面 docEditor.serviceCommand('legalLocate', payload)
    → 插件 parent.Common.Gateway.on('internalcommand')
    → Asc.plugin.executeMethod('SearchNext', ...)
    → postMessage 回页面 legal:locate-result
```

---

## 一、启动 Document Server

在项目根目录执行（统一 Docker 编排，见 [script/docker/Docker-HOWTO.md](../../script/docker/Docker-HOWTO.md)）：

```powershell
cd script/docker
docker compose --env-file docker.env up -d onlyoffice
```

`script/docker/docker-compose.yml` 已把源码插件目录挂载到 DS（**官方推荐方式**）：

```yaml
volumes:
  - ../../laby-module-legal/src/main/resources/onlyoffice-plugin/legal-locate:/var/www/onlyoffice/documentserver/sdkjs-plugins/legal-locate
```

验证 DS 可访问：

| 地址 | 期望 |
|------|------|
| http://127.0.0.1:8088/web-apps/apps/api/documents/api.js | 返回 JS |
| http://127.0.0.1:8088/sdkjs-plugins/legal-locate-v2/config.json | 返回 JSON，`guid` 为 `asc.legal-locate-v3` |
| http://127.0.0.1:8088/sdkjs-plugins/legal-locate-v2/index.html | 引用 `locate-core.js?v=20260606-v3`，**不要**出现 `code.js` |

修改插件文件后，重启 DS 服务（官方要求）：

```powershell
docker exec laby-onlyoffice supervisorctl restart ds:docservice ds:converter
```

**禁止**在插件目录放 `*.gz` 预压缩文件，否则 nginx 会优先返回旧代码。

---

## 二、配置 laby-server

`application-local.yaml`（或对应环境）：

```yaml
laby:
  legal:
    onlyoffice:
      enabled: true
      document-server-url: http://127.0.0.1:8088/
      jwt-secret: laby-onlyoffice-dev-secret-change-me   # 与 compose 中 JWT_SECRET 一致
      callback-base-url: http://host.docker.internal:48080/admin-api   # DS 容器拉合同文件
      plugin-mount-on-document-server: true   # 必须 true：pluginsData 走 8088/sdkjs-plugins
      file-token-ttl-minutes: 15
      editor-lang: zh-CN
```

说明：

| 配置项 | 谁访问 | 填什么 |
|--------|--------|--------|
| `document-server-url` | 浏览器加载 api.js | `http://127.0.0.1:8088/` |
| `callback-base-url` | **DS 容器**拉 docx | `host.docker.internal:48080/admin-api`（Win/Mac Docker） |
| `plugin-mount-on-document-server` | 浏览器加载插件 | `true` → `8088/sdkjs-plugins/legal-locate/config.json` |

`plugin-base-url` 仅在 `plugin-mount-on-document-server: false` 时使用（非官方推荐，社区版易出问题）。

**改配置后必须重启 laby-server。**

---

## 三、验证 preview-config（关键）

登录后打开审阅页，F12 → Network → 找 `preview-config` 响应，检查：

```json
"editorConfig": {
  "plugins": {
    "autostart": ["asc.legal-locate-v3"],
    "pluginsData": ["http://127.0.0.1:8088/sdkjs-plugins/legal-locate-v2/config.json"]
  }
}
```

若 `pluginsData` 仍是 `48080/admin-api/...` 或 autostart 仍是 `asc.legal-locate`（无 v2），说明 **laby-server 未重启或配置未生效**。

---

## 四、打开审阅页（清缓存）

1. **关闭**所有含 OnlyOffice 的浏览器 Tab（仅刷新不够，插件 iframe 会常驻内存）
2. 重新从合同列表进入「审阅工作台」
3. F12 → Network 确认加载：
   - `locate-core.js?v=20260606-v2`（来自 **8088**）
   - **不应**再出现 `code.js?v=10`
4. Console 应出现：`[legal-locate-v3] plugin loaded ...`
5. 点击意见「定位段落」，Console **不应**再出现 `selectBookmarkInDocument`

---

## 五、插件目录结构（与官方一致）

```
onlyoffice-plugin/legal-locate/
├── config.json          # guid: asc.legal-locate-v2, isSystem + type: system
├── index.html           # 先 plugins.js，再 locate-core.js
├── locate-core.js       # 业务逻辑（SearchNext + Gateway）
├── vendor/plugins.js    # ONLYOFFICE 官方 plugins.js 副本
└── translations/
    ├── langs.json
    └── zh-CN.json
```

`config.json` 要点（官方 system 插件）：

- `isSystem: true` + `type: "system"` → 后台 autostart，无 UI 按钮
- `EditorsSupport: ["word"]` → 仅 Word 合同
- `events: ["onDocumentReady"]`

---

## 六、常见问题

### `Permissions policy violation: unload`

OnlyOffice 8.0 内部告警，**可忽略**，不影响编辑和定位。

### `translations/langs.json 404`

已在插件目录补 `translations/`，重启 DS 后消失；不影响定位功能。

### 仍报 `selectBookmarkInDocument is not defined`

说明浏览器仍在跑**旧插件**（多为 `code.js.gz` 或旧 Tab 缓存）：

1. 删除插件目录下所有 `*.gz`
2. `supervisorctl restart ds:docservice ds:converter`
3. 重启 laby-server，确认 preview-config 指向 8088 + v2 guid
4. 关浏览器重开审阅页

### 定位不到正文

1. 确认合同为 **WORKING** 版 docx（含段落书签 `laby_p_p-*`）
2. 意见有关联段落且 `oldText`/正文片段在文档中存在
3. 定位依赖 **SearchNext 搜原文**，与书签无关

---

## 七、关闭 OnlyOffice

`enabled: false` 时工作台降级为 docx-preview，不影响审核流程。

## 关联文档

- [ONLYOFFICE 插件安装（on-premises）](https://api.onlyoffice.com/docs/plugin-and-macros/tutorials/installing/onlyoffice-docs-on-premises/)
- [SearchNext 官方 API](https://api.onlyoffice.com/docs/plugin-and-macros/interacting-with-editors/document-api/Methods/SearchNext/)
- [editorConfig.plugins](https://api.onlyoffice.com/docs/docs-api/usage-api/config/editor/plugins/)
