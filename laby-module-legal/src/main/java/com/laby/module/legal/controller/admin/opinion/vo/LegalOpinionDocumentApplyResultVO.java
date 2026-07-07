package com.laby.module.legal.controller.admin.opinion.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "意见处置后 WORKING 文档应用结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalOpinionDocumentApplyResultVO {

    @Schema(description = "是否已更新 WORKING 文档")
    private Boolean documentUpdated;

    @Schema(description = "文档 revision（OnlyOffice 刷新 key 用）")
    private String documentRevision;
}
