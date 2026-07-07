package com.laby.module.legal.controller.admin.auditrule.vo;

import com.laby.framework.excel.core.annotations.DictFormat;
import com.laby.module.legal.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 审核规则 Response VO")
@Data
public class LegalAuditRuleRespVO {

    private Long id;
    private String name;
    private Long contractTypeId;
    private String contractTypeName;
    @DictFormat(DictTypeConstants.LEGAL_CLAUSE_TYPE)
    private String clauseType;
    private Long standardClauseId;
    private String standardClauseName;
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
    private LocalDateTime createTime;

}
