package com.laby.module.legal.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * 法务合同附件 DO
 */
@TableName("legal_contract_file")
@KeySequence("legal_contract_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractFileDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 合同审核编号
     */
    private Long contractId;
    /**
     * infra 文件编号
     */
    private Long fileId;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 是否主文件
     */
    private Boolean mainFlag;
    /**
     * 文件角色：ORIGINAL / NORMALIZED_DOCX / ANNOTATED_PDF 等
     */
    private String role;
    /**
     * 文件格式：DOCX / DOC / PDF
     */
    private String format;
    /**
     * 衍生件对应的原件 infra file_id
     */
    private Long sourceFileId;
    /**
     * 转换状态（衍生件）
     */
    private Integer convertStatus;

}
