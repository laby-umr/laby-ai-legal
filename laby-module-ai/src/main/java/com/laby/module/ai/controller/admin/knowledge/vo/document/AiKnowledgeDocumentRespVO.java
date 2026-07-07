package com.laby.module.ai.controller.admin.knowledge.vo.document;

import com.laby.framework.excel.core.annotations.DictFormat;
import com.laby.module.ai.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI 知识库文档 Response VO")
@Data
public class AiKnowledgeDocumentRespVO {

    @Schema(description = "文档编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "24790")
    private Long id;

    @Schema(description = "知识库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "24790")
    private Long knowledgeId;

    @Schema(description = "文档名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Java 开发手册")
    private String name;

    @Schema(description = "文档 URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://doc.iocoder.cn")
    private String url;

    @Schema(description = "文档内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "Java 是一门面向对象的语言.....")
    private String content;

    @Schema(description = "文档内容长度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Integer contentLength;

    @Schema(description = "文档 Token 数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Integer tokens;

    @Schema(description = "分片最大 Token 数", requiredMode = Schema.RequiredMode.REQUIRED, example = "512")
    private Integer segmentMaxTokens;

    @Schema(description = "召回次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer retrievalCount;

    @Schema(description = "文档状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "入库状态", example = "0")
    @DictFormat(DictTypeConstants.AI_KNOWLEDGE_DOCUMENT_INGEST_STATUS)
    private Integer ingestStatus;

    @Schema(description = "入库失败原因", example = "向量 API 调用失败")
    private String ingestError;

    @Schema(description = "入库开始时间")
    private LocalDateTime ingestStartedAt;

    @Schema(description = "入库结束时间")
    private LocalDateTime ingestFinishedAt;

    @Schema(description = "解析引擎 code", example = "mineru")
    @DictFormat(DictTypeConstants.AI_KNOWLEDGE_PARSE_ENGINE)
    private String parseEngine;

    @Schema(description = "文档类型 code", example = "pdf")
    @DictFormat(DictTypeConstants.AI_KNOWLEDGE_DOCUMENT_TYPE)
    private String documentType;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
