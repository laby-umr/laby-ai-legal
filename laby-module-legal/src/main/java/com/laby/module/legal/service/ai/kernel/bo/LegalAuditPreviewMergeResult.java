package com.laby.module.legal.service.ai.kernel.bo;

import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 预览意见复用到正式审核的合并结果
 */
@Data
@Builder
public class LegalAuditPreviewMergeResult {

    private List<LegalAiAuditOpinionItemBO> items;

    /** 参与比对的预览条数 */
    private int previewCount;

    /** 正式审核原始条数 */
    private int formalCount;

    /** 正式与预览重复（已跳过预览补入） */
    private int dedupeCount;

    /** 从预览补入、正式未覆盖的条数 */
    private int reusedFromPreviewCount;

    public static LegalAuditPreviewMergeResult empty(List<LegalAiAuditOpinionItemBO> formal) {
        int size = formal != null ? formal.size() : 0;
        return LegalAuditPreviewMergeResult.builder()
                .items(formal != null ? formal : List.of())
                .formalCount(size)
                .previewCount(0)
                .dedupeCount(0)
                .reusedFromPreviewCount(0)
                .build();
    }

}
