package com.laby.module.legal.service.eval.bo;

import com.laby.module.legal.service.playbook.bo.LegalReviewPlanRuleBO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Playbook 黄金集单条评测用例
 */
@Data
public class LegalPlaybookEvalCaseBO {

    /** 用例 ID */
    private String caseId;

    /** 用例说明 */
    private String description;

    /** 确定性规则列表 */
    private List<LegalReviewPlanRuleBO> rules = new ArrayList<>();

    /** 段落夹具 */
    private List<LegalPlaybookEvalParagraphBO> paragraphs = new ArrayList<>();

    /** 期望结果 */
    private LegalPlaybookEvalExpectationBO expectation;

}
