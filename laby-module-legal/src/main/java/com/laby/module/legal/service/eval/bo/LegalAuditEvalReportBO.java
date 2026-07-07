package com.laby.module.legal.service.eval.bo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Playbook 黄金集评测报告
 */
@Data
@Builder
public class LegalAuditEvalReportBO {

    /** 用例总数 */
    private int totalCases;

    /** 通过用例数 */
    private int passedCases;

    /** 失败用例 ID 列表 */
    @Builder.Default
    private List<String> failedCaseIds = new ArrayList<>();

    /**
     * 通过率
     */
    public double passRate() {
        return totalCases == 0 ? 0D : (double) passedCases / totalCases;
    }

    /**
     * 是否达到最低通过率门禁
     */
    public boolean meetsThreshold(double minPassRate) {
        if (minPassRate <= 0D) {
            return true;
        }
        return passRate() + 1e-9 >= minPassRate;
    }

}
