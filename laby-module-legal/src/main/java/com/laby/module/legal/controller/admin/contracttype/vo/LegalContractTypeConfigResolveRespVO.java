package com.laby.module.legal.controller.admin.contracttype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "合同类型 - 运行时配置解析预览（CFG-001 P2）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractTypeConfigResolveRespVO {

    @Schema(description = "合同类型编号")
    private Long contractTypeId;

    @Schema(description = "合同类型名称")
    private String contractTypeName;

    @Schema(description = "知识库编号")
    private Long knowledgeId;

    @Schema(description = "知识库名称")
    private String knowledgeName;

    @Schema(description = "审核提示词来源：SKILL_PACK / DEFAULT_ROLE")
    private String auditPromptSource;

    @Schema(description = "审核聊天角色编号")
    private Long auditChatRoleId;

    @Schema(description = "审核聊天角色名称")
    private String auditChatRoleName;

    @Schema(description = "审核 systemMessage 预览（前 200 字）")
    private String auditSystemMessagePreview;

    @Schema(description = "审核 Agent 工具")
    private List<String> auditToolNames;

    @Schema(description = "对话技能包摘要")
    private LegalContractTypeSkillPackSummaryVO chatSkillPack;

}
