package com.laby.module.ai.dal.dataobject.knowledge;

import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentIngestStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI 知识库-文档 DO
 *
 * @author xiaoxin
 */
@TableName(value = "ai_knowledge_document")
@KeySequence("ai_knowledge_document_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeDocumentDO extends TenantBaseDO {

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
     * 文档名称
     */
    private String name;
    /**
     * 文件 URL
     */
    private String url;
    /**
     * 内容
     */
    private String content;
    /**
     * 文档长度
     */
    private Integer contentLength;

    /**
     * 文档 token 数量
     */
    private Integer tokens;
    /**
     * 分片最大 Token 数
     */
    private Integer segmentMaxTokens;

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

    /**
     * 入库状态（分段 + 向量化）
     * <p>
     * 枚举 {@link AiKnowledgeDocumentIngestStatusEnum}
     */
    private Integer ingestStatus;

    /**
     * 入库失败原因
     */
    private String ingestError;

    /**
     * 入库开始时间
     */
    private LocalDateTime ingestStartedAt;

    /**
     * 入库结束时间
     */
    private LocalDateTime ingestFinishedAt;

    /**
     * 解析引擎 code
     * <p>
     * 枚举 {@link com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum}
     */
    private String parseEngine;

    /**
     * 解析质量 code
     * <p>
     * 枚举 {@link com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum}
     */
    private String parseQuality;

    /**
     * 文档类型 code
     * <p>
     * 枚举 {@link com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentTypeEnum}
     */
    private String documentType;

}
