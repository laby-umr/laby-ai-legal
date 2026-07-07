package com.laby.module.legal.enums.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LegalParseStatusEnum {

    WAITING(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3),
    /** 预览可用、结构索引不完整（如 PDF 待 Phase2、扫描件） */
    PARTIAL(4),
    ;

    private final Integer status;

}
