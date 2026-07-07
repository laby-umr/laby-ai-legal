package com.laby.module.legal.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "OnlyOffice 预览配置")
@Data
public class LegalDocumentPreviewConfigRespVO {

    @Schema(description = "是否启用 OnlyOffice")
    private Boolean enabled;

    @Schema(description = "Document Server 根地址")
    private String documentServerUrl;

    @Schema(description = "DocsAPI.DocEditor 配置（含 token）")
    private Map<String, Object> config;

    @Schema(description = "当前 WORKING 文档 revision（内容 hash 前缀）")
    private String documentRevision;

    @Schema(description = "是否可编辑（OnlyOffice edit 模式）")
    private Boolean editable;
}
