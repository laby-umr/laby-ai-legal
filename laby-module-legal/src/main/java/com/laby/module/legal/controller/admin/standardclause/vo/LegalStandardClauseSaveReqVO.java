package com.laby.module.legal.controller.admin.standardclause.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 标准条款新增/修改 Request VO")
@Data
public class LegalStandardClauseSaveReqVO {

    private Long id;

    @NotBlank(message = "条款名称不能为空")
    private String name;

    private String clauseType;

    private String categoryScope;

    @NotBlank(message = "条款正文不能为空")
    private String content;

    private String referenceSource;

    private Integer status;

    private Integer sort;

}
