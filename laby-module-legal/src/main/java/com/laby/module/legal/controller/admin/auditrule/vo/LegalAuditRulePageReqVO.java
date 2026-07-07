package com.laby.module.legal.controller.admin.auditrule.vo;

import com.laby.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 审核规则分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalAuditRulePageReqVO extends PageParam {

    private String name;
    private Long contractTypeId;
    private Boolean enabled;

}
