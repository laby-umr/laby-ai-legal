package com.laby.module.legal.service.ai.kernel.bo;

import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import lombok.Builder;
import lombok.Data;

/**
 * 编排预览审核命令
 */
@Data
@Builder
public class LegalAuditPreviewCommand {

    private LegalAiPolicyBO policy;

    private Long sessionId;

    private Long fileItemId;

    private Long contractTypeId;

    private Long infraFileId;

    private String fileName;

    /** 为空时使用 {@link com.laby.module.legal.enums.ai.LegalAiAuditKernelConstants#MAX_PREVIEW_PARAGRAPHS} */
    private Integer maxParagraphs;

}
