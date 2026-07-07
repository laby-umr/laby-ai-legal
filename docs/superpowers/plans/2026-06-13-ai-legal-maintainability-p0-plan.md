# MAINT-001 实施计划

**Spec:** [2026-06-13-ai-legal-maintainability-p0-spec.md](../specs/2026-06-13-ai-legal-maintainability-p0-spec.md)

## Checklist

- [x] P0-1 法务 `CONTRACT_DELIVERABLE_INVALID` → `1_050_000_057`
- [x] P0-2 新增 `LegalAiModelResolver` + 5 处调用方替换
- [x] P0-3 删除 `AiKnowledgeSegmentServiceImpl` legacy RAG + 错误码 `KNOWLEDGE_RETRIEVAL_DISABLED`
- [x] P0-4 `AiKnowledgeSegmentController.search` 走真实 diagnostics
- [x] P0-5 单测 + `mvn test` 验证

## 实施顺序

1. Spec/Plan（本文档）
2. Legal：错误码 + ModelResolver（无跨模块依赖）
3. AI：Segment 检索单路径 + Controller 诊断
4. 编译与单测
