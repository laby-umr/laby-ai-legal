package com.laby.module.legal.service.orchestration.bo;

import lombok.Data;

/**
 * 预览审核意见摘要（Tool / 前端展示）
 */
@Data
public class LegalOrchestrationAuditPreviewItemBO {

    private Long fileItemId;

    private String fileName;

    private String title;

    private String riskLevel;

    private String clauseType;

    private String paragraphId;

    private String sourceType;

    private String content;

    private String suggestion;

}
