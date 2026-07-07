package com.laby.module.legal.service.orchestrator.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 审核单条意见（LLM 批处理输出）
 */
@Data
public class LegalAiAuditOpinionItemBO {

    private String clauseType;
    private String riskLevel;
    private String title;
    private String content;
    private String suggestion;
    @JsonProperty("paragraphId")
    private String paragraphId;
    @JsonProperty("clauseId")
    private String clauseId;
    @JsonProperty("referenceClause")
    private String referenceClause;
    @JsonProperty("sourceType")
    private String sourceType;
    @JsonProperty("sourceId")
    private String sourceId;
    @JsonProperty("evidenceRefs")
    private List<Map<String, String>> evidenceRefs;
    @JsonProperty("changeType")
    private String changeType;
    @JsonProperty("oldText")
    private String oldText;
    @JsonProperty("newText")
    private String newText;

}
