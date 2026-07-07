package com.laby.module.ai.enums.knowledge;

import com.laby.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 知识库分段层级（Parent-Child）
 */
@AllArgsConstructor
@Getter
public enum AiKnowledgeSegmentChunkLevelEnum implements ArrayValuable<Integer> {

    CHILD(0, "子块（检索）"),
    PARENT(1, "父块（上下文）");

    private final Integer level;
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(AiKnowledgeSegmentChunkLevelEnum::getLevel).toArray(Integer[]::new);

    public static AiKnowledgeSegmentChunkLevelEnum valueOfLevel(Integer level) {
        if (level == null) {
            return CHILD;
        }
        return Arrays.stream(values())
                .filter(item -> item.level.equals(level))
                .findFirst()
                .orElse(CHILD);
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
