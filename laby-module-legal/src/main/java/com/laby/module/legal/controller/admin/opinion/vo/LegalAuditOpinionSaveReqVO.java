package com.laby.module.legal.controller.admin.opinion.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 手工审核意见 Save Request VO")
@Data
public class LegalAuditOpinionSaveReqVO {

    @Schema(description = "意见编号（更新时必填）")
    private Long id;

    @Schema(description = "合同编号", required = true)
    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @Schema(description = "审核轮次，为空则取合同当前轮次")
    private Integer auditRound;

    @Schema(description = "条款类型")
    private String clauseType;

    @Schema(description = "风险等级 HIGH/MEDIUM/LOW", required = true)
    @NotBlank(message = "风险等级不能为空")
    private String riskLevel;

    @Schema(description = "标题", required = true)
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "意见内容", required = true)
    @NotBlank(message = "意见内容不能为空")
    private String content;

    @Schema(description = "修改建议")
    private String suggestion;

    @Schema(description = "段落定位")
    private String paragraphId;

}
