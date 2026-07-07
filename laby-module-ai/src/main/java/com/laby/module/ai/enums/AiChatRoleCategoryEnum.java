package com.laby.module.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 聊天角色分类（与 ai_chat_role.category 字段对应）
 */
@Getter
@AllArgsConstructor
public enum AiChatRoleCategoryEnum {

    LEGAL_CONTRACT("法务合同", "法务合同 AI 审核提示词，在「新建合同审核」中选择");

    private final String category;
    private final String description;

}
