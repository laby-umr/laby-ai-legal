package com.laby.module.legal.enums.orchestration;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 法务 AI 编排阶段（字典 legal_orchestration_phase）
 */
@Getter
@AllArgsConstructor
public enum LegalOrchestrationPhaseEnum implements ArrayValuable<String> {

    INIT("INIT"),
    FILE_REGISTERED("FILE_REGISTERED"),
    CLASSIFY_PENDING("CLASSIFY_PENDING"),
    CLASSIFY_CONFIRMED("CLASSIFY_CONFIRMED"),
    TYPE_PACKAGE_DRAFTING("TYPE_PACKAGE_DRAFTING"),
    TYPE_PACKAGE_PENDING("TYPE_PACKAGE_PENDING"),
    CREATE_PENDING("CREATE_PENDING"),
    CONTRACTS_CREATED("CONTRACTS_CREATED"),
    TRACKING("TRACKING"),
    CLOSED("CLOSED"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalOrchestrationPhaseEnum::getPhase).toArray(String[]::new);

    private final String phase;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
