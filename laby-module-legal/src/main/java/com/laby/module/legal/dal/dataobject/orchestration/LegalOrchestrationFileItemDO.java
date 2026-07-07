package com.laby.module.legal.dal.dataobject.orchestration;

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
 * 法务 AI 编排文件项 DO
 */
@TableName("legal_orchestration_file_item")
@KeySequence("legal_orchestration_file_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalOrchestrationFileItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long sessionId;

    private Long infraFileId;

    private String fileName;

    /** 见 LegalOrchestrationFileItemStatusEnum */
    private String status;

    private Long suggestedTypeId;

    private Long confirmedTypeId;

    private Long contractId;

    private Integer sort;

}
