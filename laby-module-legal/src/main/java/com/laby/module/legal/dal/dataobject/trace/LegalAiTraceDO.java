package com.laby.module.legal.dal.dataobject.trace;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务 AI 调用追踪 DO
 */
@TableName("legal_ai_trace")
@KeySequence("legal_ai_trace_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAiTraceDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String traceId;

    private Long contractId;

    private String scene;

    private Integer auditRound;

    private Long modelId;

    private String platform;

    private String status;

    private Integer promptTokens;

    private Integer completionTokens;

    private Long latencyMs;

    private Integer deterministicCount;

    private Integer opinionCount;

    /** 从编排预览补入的正式意见条数 */
    private Integer previewReuseCount;

    /** 预览与正式重复跳过条数 */
    private Integer previewDedupeCount;

    /** 是否发生模型 fallback（合同 modelId 与实解析不一致） */
    private Boolean modelFallback;

    private String errorMessage;

}
