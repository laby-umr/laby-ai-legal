package com.laby.module.legal.enums.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * BPM 流程节点 / 服务任务 Key，与 legal_contract_review.bpmn20.xml 一致
 */
@Getter
@AllArgsConstructor
public enum LegalContractTaskKeyEnum {

    PARSE_CONTRACT("parseContract"),
    AI_ROUND1("aiRound1"),
    AI_ROUND2("aiRound2"),
    OPINION_REVIEW("opinionReview"),
    REVIEW_ROUND2("reviewRound2"),
    DIRECTOR_GATEWAY("directorGateway"),
    DIRECTOR_REVIEW("directorReview"),
    FINALIZE("finalize"),
    EXPORT_REPORT("exportReport"),
    FAILED("failed"),
    ;

    private static final Set<String> OPINION_TASK_KEYS = Set.of(
            OPINION_REVIEW.key,
            REVIEW_ROUND2.key
    );

    private final String key;

    public static boolean isOpinionTaskKey(String taskKey) {
        return taskKey != null && OPINION_TASK_KEYS.contains(taskKey);
    }

    public static LegalContractTaskKeyEnum of(String taskKey) {
        if (StrUtil.isBlank(taskKey)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.key.equals(taskKey))
                .findFirst()
                .orElse(null);
    }

    public static Set<String> opinionTaskKeys() {
        return OPINION_TASK_KEYS;
    }

    public LegalContractStatusEnum toUserTaskStatus() {
        return switch (this) {
            case OPINION_REVIEW, REVIEW_ROUND2 -> LegalContractStatusEnum.OPINION_REVIEW;
            case DIRECTOR_REVIEW -> LegalContractStatusEnum.DIRECTOR_REVIEW;
            case FINALIZE -> LegalContractStatusEnum.FINALIZING;
            default -> null;
        };
    }

    public LegalContractStatusEnum toServiceActivityStatus() {
        return switch (this) {
            case PARSE_CONTRACT -> LegalContractStatusEnum.PARSING;
            case AI_ROUND1 -> LegalContractStatusEnum.AI_AUDITING;
            case AI_ROUND2 -> LegalContractStatusEnum.AI_REAUDITING;
            case EXPORT_REPORT -> LegalContractStatusEnum.FINALIZING;
            default -> null;
        };
    }

}
