package com.laby.module.legal.tool.orchestration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 法务编排 Tool 运行时上下文（全局 AI 对话）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalOrchestrationToolRuntimeContext {

    private Long conversationId;

    private Long userId;

    private Long tenantId;

    private Long modelId;

}
