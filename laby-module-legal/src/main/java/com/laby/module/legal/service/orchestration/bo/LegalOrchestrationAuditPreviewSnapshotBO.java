package com.laby.module.legal.service.orchestration.bo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 编排会话预览审核快照
 */
@Data
public class LegalOrchestrationAuditPreviewSnapshotBO {

    private String policyVersion;

    private Long modelId;

    private LocalDateTime updatedAt;

    private List<FilePreview> files;

    @Data
    public static class FilePreview {

        private Long fileItemId;

        private String fileName;

        private Integer opinionCount;

        private Integer highRiskCount;

        private List<LegalOrchestrationAuditPreviewItemBO> opinions;

    }

}
