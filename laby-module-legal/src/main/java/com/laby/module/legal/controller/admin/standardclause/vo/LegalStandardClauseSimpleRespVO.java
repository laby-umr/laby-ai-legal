package com.laby.module.legal.controller.admin.standardclause.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 标准条款精简 Response VO")
@Data
public class LegalStandardClauseSimpleRespVO {

    private Long id;
    private String name;
    private String clauseType;

}
