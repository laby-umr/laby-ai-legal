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
 * 用户事实记忆 DO（Mem0 风格，可跨合同检索）
 */
@TableName("legal_user_fact")
@KeySequence("legal_user_fact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalUserFactDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Long contractId;

    private String sessionId;

    private String content;

    private Long sourceMessageId;

    private String contentHash;

}
