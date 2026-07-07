package com.laby.module.ai.enums;

/**
 * RAG 检索黄金集评测常量
 */
public final class AiRagEvalConstants {

    /** 离线 fixture 黄金集（内嵌语料，不依赖 DB / Qdrant） */
    public static final String FIXTURE_DATASET = "eval/rag-cases-fixture.json";

    /** 在线测评用例模板（需指定 knowledgeId 后执行） */
    public static final String LIVE_DATASET = "eval/rag-cases-live.json";

    /** PDF 结构化分片黄金集（表格三索引 / 章节语义） */
    public static final String PDF_STRUCTURED_DATASET = "eval/rag-cases-pdf-structured.json";

    /** Excel 表格召回黄金集 */
    public static final String EXCEL_DATASET = "eval/rag-cases-excel.json";

    /** 模糊问法召回黄金集 */
    public static final String FUZZY_QUERY_DATASET = "eval/rag-cases-fuzzy-query.json";

    /** CI 门禁最低通过率 JVM 参数名 */
    public static final String MIN_PASS_RATE_PROPERTY = "ai.rag.eval.minPassRate";

    /** CI 门禁最低 Hit@K 通过率 JVM 参数名 */
    public static final String MIN_HIT_AT_K_RATE_PROPERTY = "ai.rag.eval.minHitAtKRate";

    /** 默认 CI 门禁通过率 */
    public static final double DEFAULT_MIN_PASS_RATE = 1.0D;

    /** 默认 Hit@K 通过率门禁 */
    public static final double DEFAULT_MIN_HIT_AT_K_RATE = 0.8D;

    private AiRagEvalConstants() {
    }

}
