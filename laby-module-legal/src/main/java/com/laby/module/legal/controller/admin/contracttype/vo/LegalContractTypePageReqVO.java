package com.laby.module.legal.controller.admin.contracttype.vo;

import com.laby.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 合同类型分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalContractTypePageReqVO extends PageParam {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "状态 0 启用 1 禁用")
    private Integer status;

}
