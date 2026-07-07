package com.laby.module.ai.dal.dataobject.knowledge;

import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库-文档分段 DO
 *
 * @author xiaoxin
 */
@TableName(value = "ai_knowledge_segment")
@KeySequence("ai_knowledge_segment_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeSegmentDO extends TenantBaseDO {

    /**
     * 向量库的编号 - 空值
     */
    public static final String VECTOR_ID_EMPTY = "";

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 知识库编号
     * <p>
     * 关联 {@link AiKnowledgeDO#getId()}
     */
    private Long knowledgeId;
    /**
     * 文档编号
     * <p>
     * 关联 {@link AiKnowledgeDocumentDO#getId()}
     */
    private Long documentId;
    /**
     * 父分段编号（结构化 Parent-Child）
     */
    private Long parentId;

    /**
     * 层级：0=child 1=parent
     * <p>
     * 枚举 {@link com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum}
     */
    private Integer chunkLevel;

    /**
     * 块类型 code
     * <p>
     * 枚举 {@link com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum}
     */
    private String blockType;

    /**
     * 章节路径（breadcrumb）
     */
    private String headingPath;

    /**
     * 起始页码
     */
    private Integer pageStart;

    /**
     * 结束页码
     */
    private Integer pageEnd;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 送入 embedding 的文本（含章节路径前缀）
     */
    private String embedText;

    /**
     * 全文检索纯文本（去 Markdown 符号）
     */
    private String sparseText;

    /**
     * 通用定位符（如 sheet:Row、Slide:3）
     */
    private String sourceLocator;

    /**
     * 切片内容长度
     */
    private Integer contentLength;

    /**
     * 向量库的编号
     */
    private String vectorId;
    /**
     * token 数量
     */
    private Integer tokens;

    /**
     * 召回次数
     */
    private Integer retrievalCount;

    /**
     * 状态
     * <p>
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

}
