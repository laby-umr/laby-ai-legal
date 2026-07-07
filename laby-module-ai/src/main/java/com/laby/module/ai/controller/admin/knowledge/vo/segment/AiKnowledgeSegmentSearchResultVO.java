package com.laby.module.ai.controller.admin.knowledge.vo.segment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 知识库段落搜索结果（含诊断）")
@Data
public class AiKnowledgeSegmentSearchResultVO {

    @Schema(description = "召回段落")
    private List<AiKnowledgeSegmentSearchRespVO> segments = new ArrayList<>();

    @Schema(description = "召回诊断（诊断开关开启时返回）")
    private AiKnowledgeRecallDiagnosticsVO diagnostics;

}
