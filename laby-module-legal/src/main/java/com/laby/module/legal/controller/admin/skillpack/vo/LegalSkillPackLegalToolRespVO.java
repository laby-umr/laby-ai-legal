package com.laby.module.legal.controller.admin.skillpack.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 法务 Agent 可选工具（CFG-001：对齐 ai_tool 与白名单）
 */
@Schema(description = "管理后台 - 法务 Agent 工具选项")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalSkillPackLegalToolRespVO {

    @Schema(description = "Spring Bean 名称", example = "legal_search_paragraphs")
    private String name;

    @Schema(description = "工具说明")
    private String description;

    @Schema(description = "是否已在 ai_tool 注册")
    private Boolean registered;

}
