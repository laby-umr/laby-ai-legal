# Playbook 黄金集评测（E7）

离线评测 Playbook 确定性引擎，**不依赖 DB / LLM**。

## 数据集

- `playbook-cases.json` — 10 条 Playbook 用例（必备/禁止/多规则/跳过 CUSTOM_LLM 等）

## 本地运行

```bash
mvn -pl laby-module-legal test "-Dtest=LegalPlaybookEvalRunnerTest"
```

## CI 门禁

默认要求 **100% 通过率**。可通过 JVM 参数调整阈值（见 `LegalEvalConstants.MIN_PASS_RATE_PROPERTY`）：

```bash
mvn -pl laby-module-legal test "-Dtest=LegalPlaybookEvalRunnerTest" \
  "-Dlegal.eval.minPassRate=0.95"
```

报告输出：`laby-module-legal/target/eval-reports/playbook-eval-report.json`

## 扩展

新增用例：编辑 `playbook-cases.json`，字段说明见 `LegalPlaybookEvalCaseBO`。
