package com.laby.module.legal.controller.admin.contracttype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 合同类型精简 Response VO")
@Data
public class LegalContractTypeSimpleRespVO {

    private Long id;
    private String name;

}
