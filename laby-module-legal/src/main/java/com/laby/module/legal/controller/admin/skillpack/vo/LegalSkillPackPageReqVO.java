package com.laby.module.legal.controller.admin.skillpack.vo;

import com.laby.framework.common.pojo.PageParam;
import com.laby.framework.common.validation.InEnum;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AI 技能包分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalSkillPackPageReqVO extends PageParam {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "场景")
    @InEnum(LegalSkillPackSceneEnum.class)
    private String scene;

    @Schema(description = "是否启用")
    private Boolean enabled;

}
