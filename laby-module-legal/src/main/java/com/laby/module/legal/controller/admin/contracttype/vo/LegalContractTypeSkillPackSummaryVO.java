package com.laby.module.legal.controller.admin.contracttype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "合同类型 - 技能包摘要")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractTypeSkillPackSummaryVO {

    @Schema(description = "技能包编号")
    private Long id;

    @Schema(description = "技能包名称")
    private String name;

    @Schema(description = "场景")
    private String scene;

    @Schema(description = "关联聊天角色编号")
    private Long chatRoleId;

    @Schema(description = "关联聊天角色名称")
    private String chatRoleName;

    @Schema(description = "工具列表")
    private List<String> toolNames;

    @Schema(description = "是否已配置")
    private Boolean configured;

}
