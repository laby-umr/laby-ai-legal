package com.laby.module.ai.service.knowledge.bo;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * AI 知识库段落搜索 Response BO
 *
 * @author 芋道源码
 */
@Data
public class AiKnowledgeSegmentSearchRespBO {

    /**
     * 段落编号
     */
    private Long id;
    /**
     * 文档编号
     */
    private Long documentId;
    /**
     * 知识库编号
     */
    private Long knowledgeId;

    /**
     * 内容
     */
    private String content;
    /**
     * 内容长度
     */
    private Integer contentLength;

    /**
     * Token 数量
     */
    private Integer tokens;

    /**
     * 相似度分数
     */
    private Double score;

    /**
     * 父分段编号
     */
    private Long parentId;

    /**
     * 块类型 code，见 {@link com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum}
     */
    private String blockType;

    /**
     * 章节路径
     */
    private String headingPath;

    /**
     * 层级：0=child 1=parent
     */
    private Integer chunkLevel;

    /**
     * 扩展上下文（Parent 回填 / 表格整表附带），用于 LLM 注入
     */
    private String expandedContent;

    /**
     * 检索注入正文的优先内容
     */
    public String getRetrievalContent() {
        return StrUtil.blankToDefault(expandedContent, content);
    }

}