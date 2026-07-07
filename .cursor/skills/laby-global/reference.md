# 全局规范参考（laby-global）

> 主 Skill：[SKILL.md](SKILL.md)

---

## 1. 完整建表模板

```sql
-- laby-{module}-{feature}.sql
-- 可重复执行
-- 执行: mysql --default-character-set=utf8mb4 -u root -p ruoyi-vue-pro < sql/mysql/laby-xxx.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `legal_xxx` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(128) NOT NULL COMMENT '名称',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态，字典 legal_xxx_status',
    `contract_id` BIGINT       NOT NULL COMMENT '合同编号，关联 legal_contract.id',
    `remark`      VARCHAR(512)          DEFAULT NULL COMMENT '备注',
    `creator`     VARCHAR(64)           DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     VARCHAR(64)           DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_legal_xxx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='法务 XXX 表';

SET FOREIGN_KEY_CHECKS = 1;
```

---

## 2. 字典脚本（幂等）

```sql
SET NAMES utf8mb4;

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('法务 XXX 状态', 'legal_xxx_status', 0, 'LegalXxxStatusEnum', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `remark` = VALUES(`remark`),
    `updater` = '1',
    `update_time` = NOW();

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
    (1, '启用', '0', 'legal_xxx_status', 0, '1', NOW(), '1', NOW(), b'0'),
    (2, '禁用', '1', 'legal_xxx_status', 0, '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
    `label` = VALUES(`label`),
    `sort` = VALUES(`sort`),
    `updater` = '1',
    `update_time` = NOW();
```

---

## 3. UNHEX 中文兜底

### 3.1 何时使用

| 场景 | 做法 |
|------|------|
| IDE/ Git 保存 UTF-8，Linux/macOS mysql utf8mb4 执行 | **直接写中文** |
| Windows CMD 重定向、部分 CI、客户端 latin1 | 中文改用 **UNHEX** |
| 已出现 `????`、`\uXXXX` 乱码 | 改 UNHEX + 确认 `--default-character-set=utf8mb4` |

### 3.2 生成 hex（UTF-8）

```bash
# Linux/macOS
echo -n '法务合同' | xxd -p
# → e6b395e58aa1e59088e5908c

# PowerShell
[BitConverter]::ToString([Text.Encoding]::UTF8.GetBytes('法务合同')).Replace('-','').ToLower()
```

### 3.3 INSERT 示例

```sql
-- 直接中文（首选）
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('法务合同状态', 'legal_contract_status', 0, '', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- UNHEX 兜底（'法务合同状态' 的 UTF-8 hex）
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES (UNHEX('E6B395E58AA1E59088E5908CE790B6E68081'), 'legal_contract_status', 0, '', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
```

### 3.4 动态 SQL 中的 COMMENT

```sql
SET @sql = IF(@col_exists = 0,
    CONCAT('ALTER TABLE `legal_contract` ADD COLUMN `title` VARCHAR(255) NOT NULL COMMENT ', QUOTE('合同标题'), ' AFTER `id`'),
    'SELECT 1');
-- 或乱码环境：COMMENT ', UNHEX('E59088E5908CE68087E98998'), '
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

---

## 4. mysql 客户端命令

```bash
# ✅ 正确
mysql --default-character-set=utf8mb4 -h 127.0.0.1 -u root -p ruoyi-vue-pro < sql/mysql/laby-xxx.sql

# ❌ 省略字符集 — Windows 下易乱码
mysql -u root -p ruoyi-vue-pro < sql/mysql/laby-xxx.sql
```

Navicat / DBeaver：连接字符集选 **utf8mb4**，SQL 文件编码 **UTF-8**。

---

## 5. 幂等模式速查

| 场景 | 模式 |
|------|------|
| 建表 | `CREATE TABLE IF NOT EXISTS` |
| 固定 id 菜单/字典 | `INSERT ... ON DUPLICATE KEY UPDATE` |
| role_menu / 关联 | `INSERT ... SELECT ... WHERE NOT EXISTS` |
| 自增 id 菜单 | 查 parent → `INSERT SELECT FROM DUAL WHERE NOT EXISTS (path)` |
| 加列 | `information_schema.COLUMNS` + `PREPARE` |
| 加唯一索引 | `information_schema.STATISTICS` + `PREPARE` |
| 业务 seed | `(tenant_id, code)` 唯一键 + `ON DUPLICATE KEY UPDATE` |

---

## 6. 内联 FQN — 完整反例清单

```java
// ❌ 全部禁止（代码体）
com.laby.module.legal.enums.ErrorCodeConstants.LEGAL_XXX
new com.laby.module.legal.dal.dataobject.LegalContractDO()
org.springframework.util.StringUtils.hasText(s)   // 应 import StringUtils
java.util.List.of(1, 2)                            // 应 import List
io.agentscope.core.middleware.MiddlewareBase        // 应 import；且仅在允许包

// ✅ JavaDoc 允许
/** @see com.laby.module.legal.service.ai.kernel.LegalAuditKernel */
```

测试类同样禁止内联 FQN。

---

## 7. Service / Controller 模板

```java
@Slf4j
@Service
@Validated
public class LegalXxxServiceImpl implements LegalXxxService {

    @Resource
    private LegalXxxMapper xxxMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createXxx(LegalXxxSaveReqVO reqVO) {
        LegalXxxDO row = BeanUtils.toBean(reqVO, LegalXxxDO.class);
        row.setStatus(LegalXxxStatusEnum.ENABLE.getStatus());
        xxxMapper.insert(row);
        log.info("[createXxx][id={}] name={}", row.getId(), row.getName());
        return row.getId();
    }
}
```

```java
@Tag(name = "管理后台 - 法务 XXX")
@RestController
@RequestMapping("/legal/xxx")
@Validated
public class LegalXxxController {

    @Resource
    private LegalXxxService xxxService;

    @PostMapping("/create")
    @Operation(summary = "创建 XXX")
    @PreAuthorize("@ss.hasPermission('legal:xxx:create')")
    public CommonResult<Long> create(@Valid @RequestBody LegalXxxSaveReqVO reqVO) {
        return success(xxxService.createXxx(reqVO));
    }
}
```

---

## 8. P3C 对照表

| 反例 | 正例 |
|------|------|
| `if (status == 1)` | `LegalXxxStatusEnum.DISABLE.getStatus().equals(status)` |
| `log.info("id=" + id)` | `log.info("[create][id={}]", id)` |
| `list.get(0)` | `CollUtil.isEmpty(list) ? null : list.get(0)` |
| `catch (Exception e) {}` | `log.warn("[x][id={}]", id, e); throw exception(...)` |
| `optional.get()` | `optional.orElseThrow(...)` |
| 返回 `null` List | `List.of()` |
| 事务内调 LLM | 事务外 + `afterCommit` |

---

## 9. Builder

```java
@Data
@Builder
public class LegalAuditKernelCommand {
    private LegalContractDO contract;
    private List<LegalContractParagraphDO> paragraphs;
    private int auditRound;
    private boolean failFast;
    private boolean playbookEnabled;
}
```

跨层 ≥4 字段或含可选 → 必须 `@Builder`；方法内 ≤3 必填 → `record`。

---

## 10. JavaDoc 示例

```java
/**
 * 合同 AI 审核内核：编排 Playbook、LLM 分批、意见合并。
 *
 * @param command 审核命令；failFast 为 true 时任一批失败即抛业务异常
 * @return 审核结果；无段落时 opinions 为空列表
 */
public LegalAuditKernelResult runFormal(LegalAuditKernelCommand command) {
    // afterCommit 启动：避免事务未提交时异步读到旧状态
}
```

---

## 11. AgentScope import 分层（与 FQN 规则一致）

| 包 | `io.agentscope` |
|----|-----------------|
| `framework/agentscope/**` | ✅ import |
| `core/llm`, `core/rag`, `controller/**` | ❌ |
| `service/**` | ⚠️ 仅门面 `AiLlmClient` |

```bash
rg "[^/]\bio\.agentscope\.[a-z]" --glob "*.java" laby-module-ai laby-module-legal
```

---

## 12. Git 提交规范（详细）

### 12.1 好 vs 差

| 差 | 好 |
|----|-----|
| `update` | `feat(legal): add contract-type CRUD API` |
| `fix bug` | `fix(legal): avoid duplicate paragraphs on re-parse` |
| `提交代码` | `refactor(ai): extract RrfFusion to standalone class` |
| `feat: xxx and yyy and zzz` | 拆成多个 commit，或 scope 明确的一个 subject |

### 12.2 带 body 的示例

```
fix(legal): mark contract failed when audit kernel throws

Persist feedback_summary with root cause so pipeline does not
stay stuck in AI_AUDITING after LLM timeout.
```

### 12.3 多文件同功能（一次 commit OK）

```
feat(legal): add standard-clause management

- LegalStandardClause CRUD + dict SQL
- Menu insert (idempotent)
- Postman folder Legal/StandardClause
```

### 12.4 PowerShell 提交示例（Agent 用）

```powershell
git add laby-module-legal/src/... sql/mysql/laby-legal-xxx.sql
git commit -m @"
feat(legal): add standard-clause management

Idempotent SQL and dict sync for legal_standard_clause_status.
"@
git status
```

### 12.5 与 Superpowers Plan 对齐

Plan 里 `- [ ] Commit` 步骤应使用与本节一致的 message，例如：

```
git commit -m "feat(ai): implement AgentScopeLlmClient"
```

### 12.6 amend / push 红线（Agent）

- **不 amend** hook 失败后的 commit；修完 **新 commit**
- **不 amend** 已 push 的 commit（除非用户明确要求 force）
- **不 push** 除非用户明确要求

---

## 13. Shell / 文件编码（Windows Agent）

### 13.1 为何 Shell 会乱码

| 原因 | 说明 |
|------|------|
| PowerShell 5.x `Get-Content` 默认 | 常按系统 ANSI / 非 UTF-8 解码 |
| `Set-Content` / `Out-File` 默认 | 可能 UTF-16 LE BOM 或 ANSI |
| CMD 代码页 | 非 65001 时中文路径/重定向出错 |
| 管道改写 | `Get-Content \| ForEach-Object { ... } \| Set-Content` 双重编码错误 |

本仓库 **Comment、JavaDoc、字典、ErrorCode message** 大量中文 → **一律不用 Shell 改写源码**。

### 13.2 Agent 工具优先级

```
读/写/改  .java .sql .vue .ts .md .yaml
    → Read / Write / StrReplace（UTF-8）

搜索代码
    → Grep / rg（只读）

编译验证
    → mvn / npm（Shell OK）

绝不
    → Get-Content + 手工改 + Set-Content 写回 Java
```

### 13.3 反例（禁止）

```powershell
# ❌ 读改写 Java — 极易乱码
(Get-Content 'LegalXxxServiceImpl.java') -replace 'old','new' | Set-Content 'LegalXxxServiceImpl.java'

# ❌ 统计行数虽只读，但大仓库慢且易误用写回；优先 rg/Glob
Get-ChildItem -Recurse *.java | ForEach-Object { Get-Content $_.FullName | Measure-Object -Line }

# ❌ CMD 重定向写 SQL
echo INSERT INTO ... 中文 ... >> script.sql
```

### 13.4 正例

```powershell
# ✅ 只读 Git
git diff -- laby-module-legal/src/...

# ✅ 只读搜索（Cursor 内置 Grep 更佳）
rg "LegalAuditKernel" laby-module-legal --glob "*.java"

# ✅ 编译
mvn -pl laby-module-legal -q test

# ✅ SQL 导入（客户端 utf8mb4，不改文件）
mysql --default-character-set=utf8mb4 -u root -p ruoyi-vue-pro < sql/mysql/laby-xxx.sql
```

### 13.5 PowerShell 强制写文件（最后手段）

```powershell
chcp 65001 | Out-Null
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()

$content = [System.IO.File]::ReadAllText('D:\...\file.sql', [System.Text.UTF8Encoding]::new())
# ... 仅 ASCII 级替换 ...
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText('D:\...\file.sql', $content, $utf8NoBom)
```

写后 **必须** 用 Read 打开确认「法务」「审核」等中文正常。

### 13.6 Git 与 BOM

- 若 `git diff` 显示整文件删除+添加且仅编码变 → 用 `git checkout -- path` 还原，再用 **Write/StrReplace** 重做
- 仓库标准：**UTF-8 无 BOM**（与 `.editorconfig` / IDE 一致）

### 13.7 与 mysql UNHEX 分工

| 步骤 | 谁负责 | 编码 |
|------|--------|------|
| 编辑 `.sql` 文件 | Agent Write/StrReplace | UTF-8 中文可读 |
| 执行 SQL | mysql CLI + utf8mb4 | 客户端连接 |
| CLI 仍乱码 | SQL 内 UNHEX | 不依赖终端显示 |
