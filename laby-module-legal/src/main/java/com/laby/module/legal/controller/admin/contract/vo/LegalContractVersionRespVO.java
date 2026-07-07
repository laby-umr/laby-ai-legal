package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同版本 Response VO")
@Data
public class LegalContractVersionRespVO {

    @Schema(description = "版本编号")
    private Long id;
    @Schema(description = "合同编号")
    private Long contractId;
    @Schema(description = "审核轮次")
    private Integer auditRound;
    @Schema(description = "版本序号")
    private Integer versionNo;
    @Schema(description = "版本类型")
    private String type;
    @Schema(description = "来源版本编号")
    private Long sourceVersionId;
    @Schema(description = "文件编号")
    private Long fileId;
    @Schema(description = "可见性 INTERNAL/EXTERNAL")
    private String visibility;
    @Schema(description = "文件哈希")
    private String immutableHash;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
