package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 合同采纳导出预检 Response VO")
@Data
public class LegalContractExportPrecheckRespVO {

    @Schema(description = "采纳意见总数")
    private Integer adoptedCount;

    @Schema(description = "可自动写回条数")
    private Integer autoWritableCount;

    @Schema(description = "冲突条数（oldText 不匹配等）")
    private Integer conflictCount;

    @Schema(description = "需人工确认条数（高风险/规则来源）")
    private Integer manualConfirmCount;

    @Schema(description = "缺失 Bookmark 的段落数")
    private Integer anchorMissingCount;

    @Schema(description = "孤立 Bookmark 数（文档有但段落表无）")
    private Integer anchorOrphanCount;

    @Schema(description = "缺失 Bookmark 的段落 ID 样例")
    private java.util.List<String> missingParagraphIds;

    @Schema(description = "孤立 Bookmark 名称样例")
    private java.util.List<String> orphanBookmarkNames;
}
