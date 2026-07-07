package com.laby.module.legal.enums.contract;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同问答回答模式
 */
@Getter
@AllArgsConstructor
public enum LegalContractChatAnswerModeEnum {

    BRIEF("BRIEF", "请用简短要点回答（3～5 条），避免冗长。", 1024),
    STANDARD("STANDARD", "请用标准篇幅回答，条理清晰。", 2048),
    DETAILED("DETAILED", "请详细分析，必要时引用合同段落编号与意见标题。", 4096),
    ;

    private final String mode;
    private final String instruction;
    private final int maxTokens;

    public static LegalContractChatAnswerModeEnum of(String mode) {
        if (StrUtil.isBlank(mode)) {
            return STANDARD;
        }
        for (LegalContractChatAnswerModeEnum item : values()) {
            if (item.mode.equalsIgnoreCase(mode)) {
                return item;
            }
        }
        return STANDARD;
    }

}
