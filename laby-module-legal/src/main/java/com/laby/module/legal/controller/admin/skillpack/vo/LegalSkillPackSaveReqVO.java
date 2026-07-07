package com.laby.module.legal.controller.admin.skillpack.vo;

import com.laby.framework.common.validation.InEnum;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI 技能包新增/修改 Request VO")
@Data
public class LegalSkillPackSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "编码", required = true)
    @NotBlank(message = "编码不能为空")
    private String code;

    @Schema(description = "名称", required = true)
    @NotBlank(message = "名称不能为空")
    private String name;

    @Schema(description = "场景 AUDIT/CHAT/PROPOSE/EXPORT_SUMMARY", required = true)
    @NotBlank(message = "场景不能为空")
    @InEnum(LegalSkillPackSceneEnum.class)
    private String scene;

    @Schema(description = "关联 ai_chat_role 编号")
    private Long chatRoleId;

    @Schema(description = "Tool 名称列表")
    private List<String> toolNames;

    @Schema(description = "MCP 客户端名称列表")
    private List<String> mcpClientNames;

    @Schema(description = "模型策略 JSON 对象")
    private String modelPolicy;

    @Schema(description = "Playbook 模板编号")
    private Long playbookId;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "是否启用")
    private Boolean enabled;

}
