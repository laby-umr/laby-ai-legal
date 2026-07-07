package com.laby.module.legal.dal.dataobject.memory;

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
 * 法务合同情节记忆 DO
 */
@TableName("legal_contract_memory")
@KeySequence("legal_contract_memory_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractMemoryDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long contractId;

    private String sessionId;

    /** 见 LegalContractMemoryTypeEnum */
    private String memoryType;

    private String content;

    private Long sourceMessageId;

    private String contentHash;

}
