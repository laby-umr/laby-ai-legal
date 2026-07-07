---
name: laby-global
description: >-
  Custom GLOBAL coding standards: Ruoyi-Pro layering, Alibaba P3C, JavaDoc/comments, dict/enum/constants,
  SQL (COMMENT, utf8mb4, idempotent inserts, UNHEX fallback), import-only NO inline FQN, Builder,
  Git commit conventions (Conventional Commits), shell/file UTF-8 rules (no shell rewrite of source).
  ALWAYS apply when writing Java/SQL/Vue or committing.
---

# 全局规范（自定义 · 通用）

**Announce when coding:** "Following laby-global."

**优先级：** 用户指令 > **本 Skill** > laby-project / laby-role

---

## 0. AI 高发问题（Review 直接打回）

| # | 问题 | 硬性要求 |
|---|------|----------|
| 1 | **内联 FQN** | 代码体禁止 `com.laby...` / `io.agentscope...`；必须顶部 `import` + 短类名 |
| 2 | **SQL 无 COMMENT** | 每个字段 + 表必须有中文 `COMMENT` |
| 3 | **非 utf8mb4** | 表/库必须 utf8mb4；脚本头 `SET NAMES utf8mb4` |
| 4 | **中文乱码** | 终端/客户端编码不可靠时用 **`UNHEX` 十六进制** 写中文 |
| 5 | **mysql 客户端编码** | 命令行执行必须带 **`--default-character-set=utf8mb4`** |
| 6 | **SQL 不可重复执行** | 裸 `INSERT` 导致重复；必须幂等（见 §3） |
| 7 | **魔法值 / 空 catch** | 枚举/常量/ErrorCode；禁止吞异常 |
| 8 | **跨层散参** | ≥4 字段用 `@Builder` Command/BO |
| 9 | **随意 commit** | 未要求不提交；message 不符合规范；混入无关文件/密钥 |
| 10 | **Shell 改源码乱码** | **禁止**用 Shell 读写/改写含中文的源码；用编辑器工具；见 §6 |

---

## 1. import 与内联 FQN（硬约束）

### 1.1 规则

- **一律**在文件顶部 `import`，代码体、测试、main **只用短类名**
- **唯一例外：** JavaDoc 里 `{@link com.laby.module.xxx.Xxx}` 可写 FQN
- **禁止：** `new com.laby.module.legal.service.Xxx()`、`mock(io.agentscope.harness.agent.HarnessAgent.class)`

### 1.2 正误对照

```java
// ❌ 内联 FQN
public com.laby.framework.common.pojo.CommonResult<Long> create() { }
throw new com.laby.framework.common.exception.ServiceException(...);
var agent = mock(io.agentscope.harness.agent.HarnessAgent.class);

// ✅ import + 短类名
import com.laby.framework.common.pojo.CommonResult;
import io.agentscope.harness.agent.HarnessAgent;
public CommonResult<Long> create() { }
mock(HarnessAgent.class);
```

### 1.3 import 分组与格式

```
package ...;

import com.laby...;          // 本项目
import io.agentscope...;     // 第三方（仅允许的分层）
import jakarta...;
import org...;

import java...;
```

- package 后 **1 空行** → import；import 后 **1 空行** → 类体
- 方法之间 **1 空行**；禁止每行代码之间都空一行

### 1.4 提交前自检

```bash
rg "[^/]\bcom\.laby\.[a-z]" --glob "*.java" laby-module-* laby-framework
rg "[^/]\bio\.agentscope\.[a-z]" --glob "*.java" laby-module-ai laby-module-legal
```

命中 **import 行以外** → 必须改。

---

## 2. SQL 通用规范

### 2.1 脚本文件头（每个 .sql 必须）

```sql
-- laby-{module}-{feature}.sql
-- 可重复执行（幂等）
-- 执行: mysql --default-character-set=utf8mb4 -u root -p 数据库名 < 本文件.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;   -- 段首；段末恢复为 1
```

### 2.2 字符集

| 项 | 要求 |
|----|------|
| 连接 | 脚本内 `SET NAMES utf8mb4;` |
| 客户端 | `mysql ... --default-character-set=utf8mb4` |
| 建表 | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` |
| 表 COMMENT | 中文，如 `COMMENT='法务合同审核'` |
| **字段 COMMENT** | **每个字段必须有**，含字典注明 `dict_type` |

```sql
`status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态，字典 legal_contract_status',
```

### 2.3 中文 UNHEX 兜底

当 SQL 文件经 **Windows CMD / 部分 CI / 非 UTF-8 终端** 执行出现 **中文乱码** 时，对 **INSERT 中的中文常量** 使用 UNHEX：

```sql
-- 优先：UTF-8 文件 + utf8mb4 客户端（可读性最好）
INSERT INTO `system_dict_type` (`name`, `type`, ...) VALUES ('法务合同状态', 'legal_contract_status', ...);

-- 兜底：UNHEX（UTF-8 字节十六进制，不含 0x 前缀）
-- '法务合同状态' → 对应 UTF-8 hex
INSERT INTO `system_dict_type` (`name`, `type`, ...)
VALUES (UNHEX('E6B395E58AA1E59088E5908CE790B6E68081'), 'legal_contract_status', ...);
```

**规则：**

- 文件本身仍保存为 **UTF-8**
- 能正常显示中文时 **优先直接写中文**；仅乱码风险场景用 UNHEX
- UNHEX 内容必须是 **UTF-8 编码** 的 hex（可用 `echo -n '中文' | xxd -p` 或线工具生成）
- `COMMENT '中文'` 在 ALTER 动态 SQL 里若乱码，同样可改为 `COMMENT UNHEX('...')` 拼接（见 reference §3.4）

### 2.4 参数绑定

- MyBatis：**`#{}`** 绑定参数
- **禁止** `${}` 拼接用户输入

---

## 3. SQL 幂等 / 递增插入（避免重复）

**原则：** 同一脚本在 dev/test/prod **可多次执行**，不产生重复行、不报错。

### 3.1 建表

```sql
CREATE TABLE IF NOT EXISTS `legal_xxx` ( ... ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='...';
```

### 3.2 固定主键的数据（菜单、字典、配置）

```sql
INSERT INTO `system_menu` (`id`, `name`, ...)
VALUES (6800, '法务合同', ...)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `permission` = VALUES(`permission`),
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0';
```

```sql
INSERT INTO `system_dict_data` (...)
VALUES (...)
ON DUPLICATE KEY UPDATE `label` = VALUES(`label`), `sort` = VALUES(`sort`);
```

### 3.3 关联表 / 无固定 id（role_menu 等）

```sql
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, ...)
SELECT 2, m.`id`, '1', NOW(), '1', NOW(), b'0', 1
FROM ( SELECT 6800 AS `id` UNION ALL SELECT 6801 ) m
WHERE NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.`role_id` = 2 AND rm.`menu_id` = m.`id` AND rm.`deleted` = b'0'
);
```

### 3.4 菜单自增 id（不占固定 id）

```sql
SET @parent_id = (SELECT `id` FROM `system_menu` WHERE `path` = '/legal' AND `deleted` = b'0' LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, ...)
SELECT '合同类型', 'legal:contract-type:query', 2, 1, @parent_id, 'contract-type', ...
FROM DUAL
WHERE @parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `system_menu`
      WHERE `deleted` = b'0' AND `parent_id` = @parent_id AND `path` = 'contract-type'
  );
```

### 3.5 已有库加列 / 加索引

**禁止** 裸 `ALTER TABLE ADD COLUMN`（第二次执行会失败）。用 `information_schema` 判断：

```sql
SET @db := DATABASE();
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'legal_contract' AND COLUMN_NAME = 'audit_role_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `legal_contract` ADD COLUMN `audit_role_id` BIGINT NULL COMMENT ''首轮审核角色'' AFTER `model_id`',
    'SELECT ''audit_role_id already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

索引同理查 `information_schema.STATISTICS`。

### 3.6 业务数据样例

- 有唯一键：`INSERT ... ON DUPLICATE KEY UPDATE`
- 无固定 id：`INSERT ... SELECT ... WHERE NOT EXISTS (...)`，按 `tenant_id` + 业务键（`code`/`name`）判重

### 3.7 禁止

| 反例 | 后果 |
|------|------|
| 裸 `INSERT` 无判重 | 重复行 |
| 重复 `ALTER ADD COLUMN` | 第二次执行失败 |
| 省略 `SET NAMES utf8mb4` | 中文乱码 |
| 字段无 COMMENT | Review 打回 |

---

## 4. Java 架构与 P3C（摘要）

| 项 | 要求 |
|----|------|
| 分层 | Controller 薄 → Service → Mapper |
| 异常 | `throw exception(ErrorCodeConstants.XXX)` |
| 事务 | `@Transactional(rollbackFor = Exception.class)`；**长 IO 不进事务** |
| 比较 | `XxxEnum.A.getStatus().equals(status)`，禁止 `==` 比较 Integer |
| 日志 | `log.info("[m][id={}]", id)`，禁止字符串拼接 |
| 集合 | 返回 `List.of()`，不返回 `null` |
| JavaDoc | public 类/方法必写；行内只写「为什么」 |
| Builder | 跨层 ≥4 字段 → `@Builder` |

---

## 5. 字典 / 枚举 / ErrorCode

- 字典四同步：SQL + `DictTypeConstants` + 枚举 + RespVO `@DictFormat`
- 禁止 Service 散落魔法数字/字符串

---

## 6. Shell 与文件编码（Agent 硬约束）

本仓库 Java / SQL / Markdown **普遍含中文**（COMMENT、JavaDoc、字典 label、日志）。  
Windows 下 **PowerShell / CMD 默认编码与 UTF-8 不一致**，用 Shell **读改写** 源文件极易 **乱码（mojibake）** 或 **丢字**。

### 6.1 改代码：禁止 Shell，必须用编辑器工具

| 操作 | ✅ 正确 | ❌ 禁止 |
|------|---------|---------|
| 读文件 | **Read** 工具 | `Get-Content` / `cat` / `type` 再人工改 |
| 改文件 | **StrReplace** / **Write** 工具 | `Set-Content` / `echo >>` / `sed -i` / here-string 重定向 |
| 批量替换 | 多次 **StrReplace** 或专用脚本（用户审阅后） | `powershell -replace` 管道写回 `.java` |
| 新建文件 | **Write**（UTF-8） | Shell heredoc 写 `.java`/`.sql`/`.vue` |

**原则：** Shell 只跑 **构建 / 测试 / 只读查询**；**不动** 含业务文本的源文件字节。

### 6.2 Shell 允许的场景

| 用途 | 示例 |
|------|------|
| 编译测试 | `mvn -pl laby-module-legal test` |
| 只读 Git | `git status`、`git diff`（不应用 diff 输出回写文件） |
| 只读搜索 | `rg`、`git grep`（优先于 `Get-Content` 扫全库） |
| 运行脚本 | `.\docs\superpowers\scripts\baseline-smoke.ps1` |
| SQL 导入 | `mysql --default-character-set=utf8mb4 ... < file.sql` |

**统计行数 / 找大文件：** 用 `rg --files`、`git ls-files`，或 **Glob + Read**；  
**不要** `Get-Content ... | Measure-Object` 扫大量 Java 再拼接写回。

### 6.3 若用户强制 Shell 碰文件（兜底）

仅当用户明确要求且无法用编辑器工具时：

```powershell
# 读
Get-Content -Path 'path\to\file.java' -Encoding utf8 -Raw

# 写（无 BOM，与仓库一致）
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText('path\to\file.java', $content, $utf8NoBom)
```

- 会话前：`chcp 65001`；`$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::UTF8`
- **禁止** 默认 `Set-Content` / `Out-File`（易 UTF-16 LE BOM 或系统 ANSI）
- 写完后 **Read 工具复核** 中文段落未损坏

### 6.4 与 SQL UNHEX 的关系

| 层级 | 手段 |
|------|------|
| **Agent 改 .sql 文件** | Write/StrReplace，文件保持 UTF-8 中文 |
| **mysql 客户端执行** | `--default-character-set=utf8mb4` + `SET NAMES utf8mb4` |
| **终端仍乱码** | INSERT 中文改 `UNHEX('...')`（见 §2.3） |

### 6.5 乱码自检（改文件后）

- JavaDoc / 注释 / 字符串里的中文是否正常
- SQL `COMMENT '...'` 是否变成 `???` 或 ``  
- 文件是否被存成 **UTF-16 BOM**（IDE 右下角编码）
- `git diff` 是否出现整文件「全删全增」（常是编码/BOM 变化）

发现乱码：**立即用 Read 对照 Git 版本**，用 Write/StrReplace **还原 UTF-8**，不要用 Shell 二次修复。

---

## 7. Git 提交规范

### 7.1 何时提交

| 规则 | 说明 |
|------|------|
| **用户明确要求** | Agent **仅**在用户说「提交 / commit」时执行 `git commit` |
| **Plan 小步提交** | Superpowers 计划中的「Commit」步骤：单 Task 完成后、**测试通过** 再提交 |
| **禁止** | 擅自提交、一次 commit 混多个不相关 Task、测试未过就提交 |

### 7.2 Commit Message 格式（Conventional Commits）

```
<type>(<scope>): <subject>

[可选 body：说明 why，1～3 行]
```

| type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（无行为变更） |
| `docs` | 文档 / Skill / spec / plan |
| `test` | 测试 |
| `chore` | 构建、脚本、杂项 |
| `build` | 依赖 / BOM / pom |
| `style` | 格式（不含逻辑变更） |
| `perf` | 性能 |

| scope（常用） | 模块 |
|---------------|------|
| `legal` | laby-module-legal |
| `ai` | laby-module-ai |
| `ui` | laby-ui |
| `framework` | laby-framework |
| `sql` | sql/mysql |
| `deploy` | docs/deploy、docker |
| `postman` | docs/postman |

**subject 规则：**

- 英文小写开头，**祈使句**，≤ 72 字符，**无句号**
- 写 **做了什么 / 为什么**，不写「update code」「fix bug」
- 与仓库近期风格一致（见 plan 中 `feat(ai): ...`）

**示例：**

```
feat(legal): migrate audit pipeline to AiLlmClient
fix(ai): prevent duplicate segments on concurrent parse
docs: add laby-global git commit conventions
test(legal): add LegalAuditKernel failFast regression
chore(sql): idempotent menu insert for contract-type page
```

### 7.3 单次提交范围

- **一个逻辑变更** 一个 commit（Controller + Service + SQL + 字典 同一功能可同 commit）
- **禁止** 同一 commit 混：无关模块 refactor + 新功能 + 仅格式化
- 新功能尽量带上：ErrorCode、字典 SQL、Postman（如有 API 变更）

### 7.4 禁止提交

- `.env`、密钥、`application-local.yaml` 中的 token/密码（若含敏感信息）
- 本地 IDE 配置、`.cursor/settings.json` 个人偏好（除非团队约定）
- 注释掉的大段死代码、调试 `console.log` / 临时 `System.out`
- 未完成的 `TODO` 占位实现（除非 WIP 分支且用户知情）

### 7.5 Agent 执行 commit 时

1. `git status` + `git diff` 确认范围
2. **只 add 相关文件**，不 `git add .` 盲加
3. 用 HEREDOC 写 message（避免 PowerShell 转义问题）
4. **不** `--no-verify`、**不** `git config`、**不** force push（除非用户明确要求）
5. commit 失败被 hook 拒绝 → **新建 commit**，不 amend（除非用户要求且满足 amend 条件）

### 7.6 与 PR

- 分支名：`feat/{feature}`、`fix/{bug}`（与 plan 一致）
- PR 标题可与首条 commit subject 相同或略扩写
- 推送 / 建 PR **仅**在用户要求时执行

更多示例 → [reference.md §12](reference.md)

---

## 8. Checklist（编码后必做）

```
[ ] 源码/SQL/文档：用 Read/Write/StrReplace 修改，未用 Shell 写回（§6）
[ ] 无内联 FQN（rg 自检）
[ ] SQL：SET NAMES utf8mb4；每字段 COMMENT；utf8mb4 表
[ ] SQL：INSERT 幂等（ON DUPLICATE / NOT EXISTS / IF NOT EXISTS）
[ ] mysql 命令含 --default-character-set=utf8mb4
[ ] 中文注释/字符串未乱码（git diff 无异常整文件替换）
[ ] mvn test 通过
[ ] （若用户要求提交）commit message 符合 §7
```

---

## 详细模板

- SQL、UNHEX、**Shell/UTF-8**、P3C、JavaDoc、Builder、Git 提交 → [reference.md](reference.md)
