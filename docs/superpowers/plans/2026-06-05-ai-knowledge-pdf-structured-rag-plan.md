# 知识库 PDF 结构化 RAG — Phase 0 / Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement task-by-task.

**Goal:** 落地高质量 PDF 解析与层级分片基础设施（解析路由、结构化模型、Hierarchical Chunker、DB/向量 metadata 扩展），保持 Tika 降级向后兼容。

**Architecture:** `DocumentParseService` 统一入口 → Router 选择 MinerU/Docling/Tika → `HierarchicalKnowledgeChunker` 按元素类型分片 → CHILD 写 Qdrant，PARENT 仅存 DB。

**Tech Stack:** Java 17, Spring Boot, Apache Tika, Hutool HTTP, Qdrant, MinerU/Docling（外置 HTTP 服务）

**Spec:** [`docs/superpowers/specs/2026-06-05-ai-knowledge-pdf-structured-rag-spec.md`](../specs/2026-06-05-ai-knowledge-pdf-structured-rag-spec.md)

---

### Task 1: 枚举 / 常量 / 配置

**Files:**
- Create: `laby-module-ai/.../enums/knowledge/AiDocumentParseEngineEnum.java`
- Create: `laby-module-ai/.../enums/knowledge/AiDocumentParseQualityEnum.java`
- Create: `laby-module-ai/.../enums/knowledge/AiKnowledgeSegmentBlockTypeEnum.java`
- Create: `laby-module-ai/.../enums/knowledge/AiKnowledgeSegmentChunkLevelEnum.java`
- Modify: `laby-module-ai/.../enums/AiDocumentSplitStrategyEnum.java`
- Modify: `laby-module-ai/.../core/rag/AiVectorStoreMetadataKeys.java`
- Create: `laby-module-ai/.../framework/document/DocumentParseProperties.java`
- Create: `laby-module-ai/.../framework/document/DocumentParseAutoConfiguration.java`
- Modify: `laby-server/.../application.yaml`

- [x] **Step 1:** 新增 4 个 knowledge 枚举（code + name + valueOfCode）
- [x] **Step 2:** 扩展 split 策略、metadata keys、错误码
- [x] **Step 3:** DocumentParseProperties + AutoConfiguration

### Task 2: 结构化领域模型

**Files:**
- Create: `core/document/AiStructuredDocumentElementTypeEnum.java`
- Create: `core/document/AiStructuredDocumentElement.java`
- Create: `core/document/AiStructuredDocument.java`
- Create: `core/document/AiStructuredDocumentParseResult.java`
- Create: `service/knowledge/bo/AiKnowledgeChunkBO.java`

- [x] **Step 1:** 元素类型枚举与 JSON 字段映射
- [x] **Step 2:** ParseResult 含 engine/quality/markdown/elements

### Task 3: 解析客户端

**Files:**
- Create: `framework/document/DocumentParseClient.java`
- Create: `framework/document/TikaDocumentParseClient.java`
- Create: `framework/document/AbstractHttpDocumentParseClient.java`
- Create: `framework/document/HttpMinerUDocumentParseClient.java`
- Create: `framework/document/HttpDoclingDocumentParseClient.java`
- Create: `framework/document/DocumentParseRouter.java`
- Create: `framework/document/DocumentParseService.java`

- [x] **Step 1:** Tika 降级实现（quality=low）
- [x] **Step 2:** HTTP 客户端解析归一化 JSON
- [x] **Step 3:** Router 按扩展名 + 配置选引擎，失败降级 Tika

### Task 4: 层级分片器

**Files:**
- Create: `service/knowledge/splitter/HierarchicalKnowledgeChunker.java`
- Test: `HierarchicalKnowledgeChunkerTest.java`
- Test: `DocumentParseRouterTest.java`

- [x] **Step 1:** 表格独立 chunk、heading breadcrumb
- [x] **Step 2:** parent/child 临时 key 关联
- [x] **Step 3:** 无 elements 时 semantic 降级

### Task 5: DB + DO 扩展

**Files:**
- Create: `sql/mysql/laby-ai-knowledge-structured-chunk.sql`
- Modify: `AiKnowledgeDocumentDO.java`
- Modify: `AiKnowledgeSegmentDO.java`

- [x] **Step 1:** 可重复执行 migration
- [x] **Step 2:** DO 字段与枚举 Javadoc 引用

### Task 6: Service 接入

**Files:**
- Modify: `AiKnowledgeDocumentServiceImpl.java`
- Modify: `AiKnowledgeSegmentService.java`
- Modify: `AiKnowledgeSegmentServiceImpl.java`
- Modify: `ErrorCodeConstants.java`

- [x] **Step 1:** readUrl → DocumentParseService（可配置关闭走原 Tika）
- [x] **Step 2:** createKnowledgeSegmentByParseResultAsync
- [x] **Step 3:** writeVectorStore 使用 embedText + 扩展 metadata；parent 默认不 embed

### Task 7: 验证（Phase 0）

- [x] **Step 1:** Phase 0 单测 6/6 通过
- [x] **Step 2:** 编译通过

### Task 8: Phase 1 — 表格三索引

- [x] `MarkdownTableChunkSupport`（行 JSON + 规则摘要）
- [x] `HierarchicalKnowledgeChunker.buildTableChunks`（WHOLE / ROW / SUMMARY）
- [x] 配置 `table-row-index-enabled` / `table-summary-enabled`

### Task 9: Phase 1 — 检索 Parent 回填

- [x] `AiKnowledgeSegmentSearchContextSupport` + `getRetrievalContent()`
- [x] `AiChatMessageServiceImpl` / `LegalAuditContextServiceImpl` 使用扩展上下文
- [x] 配置 `parent-backfill-enabled`

### Task 10: Phase 1 — MinerU 适配层

- [x] `script/document-parse/laby-parse-adapter`（FastAPI + pdfplumber）
- [x] `docs/deploy/mineru-docker-compose.yml`

### Task 11: Phase 1 — RAG Eval PDF 黄金集

- [x] `rag-cases-pdf-structured.json`（12 条）
- [x] `AiRagEvalRunner.runPdfStructuredDataset()` — 12/12 通过

### Task 12: 验证（Phase 1）

- [x] Phase 1 相关单测 9/9 通过（含 PDF 黄金集）
- [ ] SQL 在目标库执行（`laby-ai-knowledge-structured-chunk.sql`）
