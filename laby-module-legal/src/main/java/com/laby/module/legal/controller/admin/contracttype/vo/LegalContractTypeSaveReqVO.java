package com.laby.module.legal.controller.admin.contracttype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 合同类型新增/修改 Request VO")
@Data
public class LegalContractTypeSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "名称", example = "采购合同")
    @NotBlank(message = "名称不能为空")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "关联知识库编号")
    private Long knowledgeId;

    @Schema(description = "默认审核 SkillPack 编号")
    private Long defaultSkillPackIdAudit;

    @Schema(description = "默认对话 SkillPack 编号")
    private Long defaultSkillPackIdChat;

    @Schema(description = "状态 0 启用 1 禁用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

}
