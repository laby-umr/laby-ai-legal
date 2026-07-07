package com.laby.module.ai.enums.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 知识库召回路径
 */
@AllArgsConstructor
@Getter
public enum AiKnowledgeRecallPathEnum {

    DENSE("dense", "向量检索"),
    SPARSE("sparse", "全文检索"),
    BLOCK_ROUTE("block_route", "块类型路由"),
    MULTI_QUERY("multi_query", "多查询扩展"),
    RERANK("rerank", "重排序"),
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String name;

    public static AiKnowledgeRecallPathEnum valueOfCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(UNKNOWN);
    }

}
