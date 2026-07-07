package com.laby.module.legal.service.skillpack;

import java.util.Set;

/**
 * 运行时允许的 Agent Tool 名称白名单
 */
public final class LegalSkillPackToolNames {

    public static final Set<String> ALLOWED = Set.of(
            "legal_compare_audit_rounds",
            "legal_propose_skip_paragraph",
            "legal_get_audit_report",
            "legal_get_contract_meta",
            "legal_get_audit_opinions",
            "legal_search_paragraphs",
            "legal_search_knowledge",
            "legal_propose_adopt_opinion",
            "legal_adopt_opinion",
            "legal_batch_adopt_pending_opinions"
    );

    private LegalSkillPackToolNames() {
    }

}
