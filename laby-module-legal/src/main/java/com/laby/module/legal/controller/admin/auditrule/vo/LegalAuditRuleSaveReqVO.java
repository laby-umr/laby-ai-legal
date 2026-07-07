package com.laby.module.legal.controller.admin.auditrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 审核规则新增/修改 Request VO")
@Data
public class LegalAuditRuleSaveReqVO {

    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    private Long contractTypeId;
    private String clauseType;
    private Long standardClauseId;
    private String ruleContent;
    private Integer priority;
    private Boolean enabled;
    private String description;
    private String ruleType;
    private String matchPattern;
    private String matchType;
    private String riskLevel;
    private String actionOnHit;
    private Integer playbookVersion;

}
