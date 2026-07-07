package com.laby.module.legal.controller.admin.contract.vo;

import com.laby.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 合同情节记忆分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalContractMemoryPageReqVO extends PageParam {

    private Long contractId;

    private String sessionId;

    private String memoryType;

    private String content;

}
