package com.laby.module.legal.service.ai.kernel;

import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelCommand;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditKernelResult;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditPreviewCommand;

/**
 * 法务 AI 审核内核：Playbook + LLM 统一入口（正式 / 预览）
 */
public interface LegalAuditKernel {

    /**
     * 正式审核：需已落库合同与段落
     */
    LegalAuditKernelResult runFormal(LegalAuditKernelCommand command);

    /**
     * 预览审核：临时解析文件，不写 contract / opinion 表
     */
    LegalAuditKernelResult runPreview(LegalAuditPreviewCommand command);

}
