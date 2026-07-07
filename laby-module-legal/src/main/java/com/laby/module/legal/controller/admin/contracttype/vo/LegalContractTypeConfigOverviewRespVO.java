package com.laby.module.legal.controller.admin.contracttype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "合同类型 - 配置中枢概览（CFG-001 P1）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractTypeConfigOverviewRespVO {

    @Schema(description = "合同类型编号")
    private Long contractTypeId;

    @Schema(description = "合同类型名称")
    private String contractTypeName;

    @Schema(description = "知识库编号")
    private Long knowledgeId;

    @Schema(description = "知识库名称")
    private String knowledgeName;

    @Schema(description = "生效审核规则数（含全局）")
    private Integer enabledAuditRuleCount;

    @Schema(description = "审核技能包")
    private LegalContractTypeSkillPackSummaryVO auditSkillPack;

    @Schema(description = "对话技能包")
    private LegalContractTypeSkillPackSummaryVO chatSkillPack;

    @Schema(description = "配置检查清单")
    private List<LegalContractTypeConfigCheckItemVO> checklist;

}
