package com.laby.module.legal.controller.admin.standardclause.vo;

import com.laby.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 标准条款分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalStandardClausePageReqVO extends PageParam {

    private String name;
    private String clauseType;
    private String categoryScope;
    private Integer status;

}
