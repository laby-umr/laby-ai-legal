package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 法务合同文件上传 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractUploadRespVO {

    @Schema(description = "文件编号")
    private Long fileId;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "访问地址")
    private String url;

}
