package com.laby.module.legal.controller.admin.trace.vo;



import com.laby.framework.common.pojo.PageParam;

import com.laby.framework.common.validation.InEnum;

import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;

import com.laby.module.legal.enums.trace.LegalAiTraceStatusEnum;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import lombok.EqualsAndHashCode;



@Schema(description = "管理后台 - AI 追踪分页 Request VO")

@Data

@EqualsAndHashCode(callSuper = true)

public class LegalAiTracePageReqVO extends PageParam {



    @Schema(description = "合同编号")

    private Long contractId;



    @Schema(description = "场景")

    @InEnum(LegalSkillPackSceneEnum.class)

    private String scene;



    @Schema(description = "状态")

    @InEnum(LegalAiTraceStatusEnum.class)

    private String status;



}

