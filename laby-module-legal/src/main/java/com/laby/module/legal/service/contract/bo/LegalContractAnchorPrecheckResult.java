package com.laby.module.legal.service.contract.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Bookmark 锚点与段落表一致性预检结果。
 */
@Data
public class LegalContractAnchorPrecheckResult {

    private int missingCount;
    private int orphanCount;
    private List<String> missingParagraphIds = new ArrayList<>();
    private List<String> orphanBookmarkNames = new ArrayList<>();

    public static LegalContractAnchorPrecheckResult empty() {
        return new LegalContractAnchorPrecheckResult();
    }

}
