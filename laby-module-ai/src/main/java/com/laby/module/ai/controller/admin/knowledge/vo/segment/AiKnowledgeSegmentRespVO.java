package com.laby.module.ai.controller.admin.knowledge.vo.segment;

import com.laby.framework.excel.core.annotations.DictFormat;
import com.laby.module.ai.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 知识库文档分片 Response VO")
@Data
public class AiKnowledgeSegmentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "24790")
    private Long id;

    @Schema(description = "文档编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "24790")
    private Long documentId;

    @Schema(description = "知识库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "24790")
    private Long knowledgeId;

    @Schema(description = "父分段编号", example = "1024")
    private Long parentId;

    @Schema(description = "层级 0=子块 1=父块", example = "0")
    @DictFormat(DictTypeConstants.AI_KNOWLEDGE_SEGMENT_CHUNK_LEVEL)
    private Integer chunkLevel;

    @Schema(description = "块类型 code", example = "table_row")
    @DictFormat(DictTypeConstants.AI_KNOWLEDGE_SEGMENT_BLOCK_TYPE)
    private String blockType;

    @Schema(description = "章节路径", example = "第一章 > 付款条款")
    private String headingPath;

    @Schema(description = "起始页码", example = "1")
    private Integer pageStart;

    @Schema(description = "结束页码", example = "2")
    private Integer pageEnd;

    @Schema(description = "定位符", example = "Sheet1:Row3")
    private String sourceLocator;

    @Schema(description = "向量库编号", example = "1858496a-1dde-4edf-a43e-0aed08f37f8c")
    private String vectorId;

    @Schema(description = "切片内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "Java 开发手册")
    private String content;

    @Schema(description = "送入 embedding 的文本")
    private String embedText;

    @Schema(description = "全文检索用 sparse 文本")
    private String sparseText;

    @Schema(description = "切片内容长度", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Integer contentLength;

    @Schema(description = "token 数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Integer tokens;

    @Schema(description = "召回次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer retrievalCount;

    @Schema(description = "文档状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createTime;

}