package com.laby.module.legal.enums.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同段落向量索引状态
 */
@Getter
@AllArgsConstructor
public enum LegalParagraphEmbeddingStatusEnum {

    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    private final String status;

}
