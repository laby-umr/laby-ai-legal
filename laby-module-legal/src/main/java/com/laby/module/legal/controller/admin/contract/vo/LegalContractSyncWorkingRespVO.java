package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OnlyOffice forceSave 后 WORKING 同步状态（DELIV-001 §15.2）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WORKING 同步结果")
public class LegalContractSyncWorkingRespVO {

    @Schema(description = "WORKING 文档 revision（内容 hash 前缀，供 OnlyOffice key 刷新）")
    private String revision;

    @Schema(description = "WORKING infra 文件编号")
    private Long workingFileId;

}
