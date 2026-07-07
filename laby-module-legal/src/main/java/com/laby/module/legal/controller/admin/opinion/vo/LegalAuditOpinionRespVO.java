package com.laby.module.legal.controller.admin.opinion.vo;

import com.laby.framework.excel.core.annotations.DictFormat;
import com.laby.module.legal.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 审核意见 Response VO")
@Data
public class LegalAuditOpinionRespVO {

    private Long id;
    private Long contractId;
    private Integer auditRound;
    private String clauseType;

    @DictFormat(DictTypeConstants.LEGAL_RISK_LEVEL)
    private String riskLevel;

    private String title;
    private String content;
    private String suggestion;
    private String paragraphId;
    @Schema(description = "条款定位 c-n")
    private String clauseId;
    @Schema(description = "对照的标准条款摘要")
    private String referenceClause;
    @Schema(description = "意见来源类型：AI/RULE/STANDARD_CLAUSE/MANUAL")
    private String sourceType;
    @Schema(description = "意见来源主键，如规则ID/条款ID")
    private String sourceId;
    @Schema(description = "来源版本号或发布时间戳")
    private String sourceVersion;
    @Schema(description = "该意见对应的合同版本ID")
    private Long fromVersionId;
    @Schema(description = "改写类型：REPLACE/INSERT_BEFORE/INSERT_AFTER/DELETE/NO_CHANGE")
    private String changeType;
    @Schema(description = "改写前文本（用于冲突校验）")
    private String oldText;
    @Schema(description = "改写后文本（用于导出新合同）")
    private String newText;
    @Schema(description = "结构化证据引用 JSON")
    private String evidenceRefs;
    private Integer status;

}
