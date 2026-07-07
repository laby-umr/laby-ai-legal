package com.laby.module.legal.service.auditrule.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 审核批次上下文（规则 + RAG 结构化结果，用于 prompt 与证据落库）。
 */
@Data
public class LegalAuditContextResult {

    private String supplementMarkdown = "";
    private List<RuleRef> rules = new ArrayList<>();
    private List<KnowledgeRef> knowledgeSegments = new ArrayList<>();
    private String ragQuery;

    @Data
    public static class RuleRef {
        private Long ruleId;
        private Long standardClauseId;
        private String name;
        private String clauseType;
    }

    @Data
    public static class KnowledgeRef {
        private Long segmentId;
        private Long documentId;
        private Double score;
        private String excerpt;
    }

}
