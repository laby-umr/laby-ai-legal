package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 用户事实记忆新增/修改 Request VO")
@Data
public class LegalUserFactSaveReqVO {

    private Long id;

    @NotNull(message = "用户编号不能为空")
    private Long userId;

    private Long contractId;

    private String sessionId;

    @NotBlank(message = "事实内容不能为空")
    private String content;

}
