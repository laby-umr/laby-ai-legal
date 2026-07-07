# RAG 检索黄金集评测

离线评测 RAG 检索链路（Hit@K / MRR / Recall@K），fixture 模式**不依赖 DB / Qdrant / Embedding API**。

## 数据集

| 文件 | 说明 |
|------|------|
| `rag-cases-fixture.json` | 10 条内嵌语料用例（CI 默认跑） |
| `rag-cases-pdf-structured.json` | 12 条 PDF 结构化/表格三索引用例 |
| `rag-cases-live.json` | 5 条在线用例模板（需指定 `knowledgeId`） |

## 指标

- **Hit@K**：期望 segmentId 是否出现在 TopK
- **MRR**：首个命中排名的倒数均值
- **Recall@K**：期望 segment 被召回比例
- **Pass**：Hit@K + Recall@K + 关键词期望 + Top1 分数（如有）全部满足

## 本地运行（fixture）

```bash
mvn -pl laby-module-ai test "-Dtest=AiRagEvalRunnerTest"
```

报告输出：`laby-module-ai/target/eval-reports/rag-eval-report.json`

## 在线测评（真实知识库）

需应用已启动、Qdrant 可连、知识库已入库：

```bash
# 管理端 API（需 ai:knowledge:query 权限）
POST /ai/knowledge/rag-eval?knowledgeId={id}
Content-Type: application/json

# 可选：自定义用例；不传则使用 rag-cases-live.json
[{ "caseId": "custom-1", "query": "保密义务", "topK": 5, "expectation": { "expectedContentContains": ["保密"] } }]
```

## CI 门禁

```bash
mvn -pl laby-module-ai test "-Dtest=AiRagEvalRunnerTest" \
  "-Dai.rag.eval.minPassRate=0.9" \
  "-Dai.rag.eval.minHitAtKRate=0.8"
```

## 扩展

新增 fixture 用例：编辑 `rag-cases-fixture.json`，字段见 `AiRagEvalCaseBO`。
