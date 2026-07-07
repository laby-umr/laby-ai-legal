package com.laby.module.ai.core.document;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 结构化文档元素（MinerU / Docling 归一化输出）
 */
@Data
@Accessors(chain = true)
public class AiStructuredDocumentElement {

    /** 元素类型 code，见 {@link AiStructuredDocumentElementTypeEnum} */
    private String type;

    private String text;

    private String markdown;

    private String html;

    private String caption;

    private String description;

    private String imageUrl;

    private Integer page;

    /** 标题层级（1=章，2=节…） */
    private Integer level;

}
