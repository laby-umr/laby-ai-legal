package com.laby.module.ai.service.knowledge.bo;

import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentChunkLevelEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 结构化分片中间对象（持久化前）
 */
@Data
@Accessors(chain = true)
public class AiKnowledgeChunkBO {

    /** 展示 / 落库正文 */
    private String content;

    /** 送入 embedding 的文本（含 breadcrumb） */
    private String embedText;

    private AiKnowledgeSegmentBlockTypeEnum blockType = AiKnowledgeSegmentBlockTypeEnum.TEXT;

    private AiKnowledgeSegmentChunkLevelEnum chunkLevel = AiKnowledgeSegmentChunkLevelEnum.CHILD;

    /** 本块临时 key（父块用于被子块引用） */
    private String tempKey;

    /** 持久化前关联父块的临时 key */
    private String parentTempKey;

    private String headingPath;

    private Integer pageStart;

    private Integer pageEnd;

    /** 是否写入向量库 */
    private boolean embedEnabled = true;

}
