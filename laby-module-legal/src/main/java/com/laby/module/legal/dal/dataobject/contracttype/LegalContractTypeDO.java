package com.laby.module.legal.dal.dataobject.contracttype;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务合同类型 DO
 */
@TableName("legal_contract_type")
@KeySequence("legal_contract_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractTypeDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 类型名称
     */
    private String name;
    /**
     * 编码
     */
    private String code;
    /**
     * 说明
     */
    private String description;
    /**
     * 优先关联 AI 知识库编号
     */
    private Long knowledgeId;
    /**
     * 默认审核 SkillPack 编号
     */
    private Long defaultSkillPackIdAudit;
    /**
     * 默认对话 SkillPack 编号
     */
    private Long defaultSkillPackIdChat;
    /**
     * 状态
     *
     * 枚举类 {@link com.laby.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;

}
