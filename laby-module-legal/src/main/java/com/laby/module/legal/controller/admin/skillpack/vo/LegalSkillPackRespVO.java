package com.laby.module.legal.controller.admin.skillpack.vo;



import com.laby.framework.excel.core.annotations.DictFormat;

import com.laby.module.legal.enums.DictTypeConstants;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



import java.time.LocalDateTime;

import java.util.List;



@Schema(description = "管理后台 - AI 技能包 Response VO")

@Data

public class LegalSkillPackRespVO {



    @Schema(description = "编号")

    private Long id;



    @Schema(description = "唯一编码")

    private String code;



    @Schema(description = "展示名")

    private String name;



    @Schema(description = "适用场景")

    @DictFormat(DictTypeConstants.LEGAL_SKILL_PACK_SCENE)

    private String scene;



    @Schema(description = "关联 AI 角色编号")

    private Long chatRoleId;



    @Schema(description = "Tool 名称列表")

    private List<String> toolNames;



    @Schema(description = "MCP 客户端名称列表")

    private List<String> mcpClientNames;



    @Schema(description = "模型策略 JSON")

    private String modelPolicy;



    @Schema(description = "Playbook 模板编号")

    private Long playbookId;



    @Schema(description = "说明")

    private String description;



    @Schema(description = "是否启用")

    private Boolean enabled;



    @Schema(description = "版本号")

    private Integer version;



    @Schema(description = "创建时间")

    private LocalDateTime createTime;



}

