package com.laby.module.legal.enums.agent;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent 写操作提案类型
 */
@Getter
@AllArgsConstructor
public enum LegalAgentProposalActionEnum {

    ADOPT_OPINION("ADOPT_OPINION"),
    SKIP_PARAGRAPH("SKIP_PARAGRAPH"),
    CLASSIFY_CONFIRM("CLASSIFY_CONFIRM"),
    CREATE_TYPE_PACKAGE("CREATE_TYPE_PACKAGE"),
    CREATE_CONTRACTS_BATCH("CREATE_CONTRACTS_BATCH"),
    ;

    private final String action;

    public static LegalAgentProposalActionEnum of(String action) {
        if (StrUtil.isBlank(action)) {
            return null;
        }
        for (LegalAgentProposalActionEnum item : values()) {
            if (item.action.equalsIgnoreCase(action)) {
                return item;
            }
        }
        return null;
    }

}
