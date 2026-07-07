package com.laby.module.legal.framework.agentscope.subagent;

import io.agentscope.harness.agent.subagent.SubagentDeclaration;

import java.util.List;

/**
 * 法务审核 Sub-agent 声明（只读 Tool 子集）。
 */
public final class LegalAuditSubagentDeclarations {

    private static final List<String> CLAUSE_BATCH_REVIEWER_TOOLS = List.of(
            "legal_get_contract_meta",
            "legal_search_paragraphs",
            "legal_get_audit_opinions"
    );

    private LegalAuditSubagentDeclarations() {
    }

    /**
     * 条款批审子 Agent：按段落批次给出风险分类与批审顺序建议。
     */
    public static SubagentDeclaration clauseBatchReviewer(String model) {
        return SubagentDeclaration.builder()
                .name("clause-batch-reviewer")
                .description("只读子 Agent：分析合同段落批次，输出条款类型与风险优先级建议")
                .model(model)
                .maxIters(6)
                .tools(CLAUSE_BATCH_REVIEWER_TOOLS)
                .inlineAgentsBody("""
                        你是法务合同条款批审助手。请基于主 Agent 提供的段落摘要：
                        1. 识别每批段落的条款类型；
                        2. 评估相对风险优先级（高/中/低）；
                        3. 给出建议的批审顺序（JSON 数组，含 paragraphId、clauseType、priority）。
                        仅使用只读 Tool，不要提议写操作。
                        """)
                .build();
    }

}
