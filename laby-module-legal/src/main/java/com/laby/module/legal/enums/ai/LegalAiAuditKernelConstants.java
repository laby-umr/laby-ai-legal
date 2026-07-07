package com.laby.module.legal.enums.ai;

/**
 * 法务 AI 审核内核常量
 */
public final class LegalAiAuditKernelConstants {

    /** 预览审核最大段落数（控制成本） */
    public static final int MAX_PREVIEW_PARAGRAPHS = 40;

    /** 审核输出 token 下限 */
    public static final int MIN_AUDIT_MAX_TOKENS = 4096;

    /** 审核输出 token 上限 */
    public static final int MAX_AUDIT_MAX_TOKENS = 8192;

    /** 每批 LLM 审核段落数 */
    public static final int MAX_PARAGRAPHS_PER_REQUEST = 5;

    private LegalAiAuditKernelConstants() {
    }

}
