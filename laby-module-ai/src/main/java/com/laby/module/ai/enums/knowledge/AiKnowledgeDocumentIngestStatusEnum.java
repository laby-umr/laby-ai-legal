package com.laby.module.ai.enums.knowledge;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI 知识库文档入库（分段+向量化）状态
 */
@AllArgsConstructor
@Getter
public enum AiKnowledgeDocumentIngestStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待处理"),
    SPLITTING(10, "分段中"),
    EMBEDDING(20, "向量化中"),
    SUCCESS(30, "已完成"),
    FAILED(40, "失败");

    private final Integer status;
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(AiKnowledgeDocumentIngestStatusEnum::getStatus).toArray(Integer[]::new);

    public static AiKnowledgeDocumentIngestStatusEnum valueOfStatus(Integer status) {
        if (status == null) {
            return PENDING;
        }
        for (AiKnowledgeDocumentIngestStatusEnum value : values()) {
            if (value.status.equals(status)) {
                return value;
            }
        }
        return PENDING;
    }

    public static boolean isRunning(Integer status) {
        AiKnowledgeDocumentIngestStatusEnum s = valueOfStatus(status);
        return s == SPLITTING || s == EMBEDDING;
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
