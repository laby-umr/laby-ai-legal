package com.laby.module.legal.enums.orchestration;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 法务编排文件项状态（字典 legal_orchestration_file_status）
 */
@Getter
@AllArgsConstructor
public enum LegalOrchestrationFileItemStatusEnum implements ArrayValuable<String> {

    REGISTERED("REGISTERED"),
    CLASSIFIED("CLASSIFIED"),
    MAPPED("MAPPED"),
    CREATED("CREATED"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(LegalOrchestrationFileItemStatusEnum::getStatus).toArray(String[]::new);

    private final String status;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
