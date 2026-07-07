package com.laby.module.legal.controller.admin.skillpack.vo;



import com.laby.framework.excel.core.annotations.DictFormat;

import com.laby.module.legal.enums.DictTypeConstants;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



@Schema(description = "管理后台 - AI 技能包精简 Response VO")

@Data

public class LegalSkillPackSimpleRespVO {



    @Schema(description = "编号")

    private Long id;



    @Schema(description = "唯一编码")

    private String code;



    @Schema(description = "展示名")

    private String name;



    @Schema(description = "适用场景")

    @DictFormat(DictTypeConstants.LEGAL_SKILL_PACK_SCENE)

    private String scene;



}

