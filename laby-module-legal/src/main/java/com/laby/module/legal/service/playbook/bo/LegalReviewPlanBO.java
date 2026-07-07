package com.laby.module.legal.service.playbook.bo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 合同审阅 Playbook（ReviewPlan）
 */
@Data
@Builder
public class LegalReviewPlanBO {

    private Long contractTypeId;
    private Integer version;
    @Builder.Default
    private List<LegalReviewPlanRuleBO> deterministicRules = new ArrayList<>();
    @Builder.Default
    private List<LegalReviewPlanRuleBO> llmRules = new ArrayList<>();

}
