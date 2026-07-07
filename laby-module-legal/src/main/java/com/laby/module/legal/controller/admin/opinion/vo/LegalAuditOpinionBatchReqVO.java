package com.laby.module.legal.controller.admin.opinion.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 审核意见批量操作 Request VO")
@Data
public class LegalAuditOpinionBatchReqVO {

    @Schema(description = "意见编号列表", required = true)
    @NotEmpty(message = "意见编号列表不能为空")
    private List<Long> ids;

}
