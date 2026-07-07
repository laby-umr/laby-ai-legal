package com.laby.module.legal.service.contract.bo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 审核意见草稿（确定性引擎 / AI 解析统一结构）
 */
@Data
@Builder
public class LegalAuditOpinionDraftBO {

    private String clauseType;
    private String riskLevel;
    private String title;
    private String content;
    private String suggestion;
    private String paragraphId;
    private String clauseId;
    private String referenceClause;
    private String sourceType;
    private String sourceId;
    private String changeType;
    private String oldText;
    private String newText;
    private List<Map<String, String>> evidenceRefs;

}
