package com.laby.module.legal.dal.dataobject.embedding;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 法务合同段落向量索引 DO
 */
@TableName("legal_contract_paragraph_embedding")
@KeySequence("legal_contract_paragraph_embedding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractParagraphEmbeddingDO extends TenantBaseDO {

    public static final String VECTOR_ID_EMPTY = "";

    @TableId
    private Long id;

    private Long contractId;

    /** 段落业务 ID，如 p-12 */
    private String paragraphId;

    /** 段落表主键 */
    private Long paragraphDbId;

    /** 向量库文档 ID */
    private String vectorId;

    /** 段落正文 MD5 */
    private String textHash;

    /** 正文预览 */
    private String contentPreview;

    /** PENDING / SUCCESS / FAILED */
    private String status;

}
