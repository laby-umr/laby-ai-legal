package com.laby.module.ai.dal.dataobject.knowledge;

import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库 DO
 *
 * @author xiaoxin
 */
@TableName(value = "ai_knowledge", autoResultMap = true)
@KeySequence("ai_knowledge_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 知识库名称
     */
    private String name;
    /**
     * 知识库描述
     */
    private String description;

    /**
     * 向量模型编号
     *
     * 关联 {@link AiModelDO#getId()}
     */
    private Long embeddingModelId;
    /**
     * 模型标识
     *
     * 冗余 {@link AiModelDO#getModel()}
     */
    private String embeddingModel;

    /**
     * topK
     */
    private Integer topK;
    /**
     * 相似度阈值
     */
    private Double similarityThreshold;

    /**
     * 状态
     * <p>
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

}
