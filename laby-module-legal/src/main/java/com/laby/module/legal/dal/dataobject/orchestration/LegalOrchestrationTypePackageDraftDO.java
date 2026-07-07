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

@TableName("legal_orchestration_type_package_draft")
@KeySequence("legal_orchestration_type_package_draft_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalOrchestrationTypePackageDraftDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long sessionId;

    private Long userId;

    private String name;

    private String code;

    private String description;

    private String contentJson;

    /** DRAFT / PUBLISHED */
    private String status;

    private Long contractTypeId;

}
