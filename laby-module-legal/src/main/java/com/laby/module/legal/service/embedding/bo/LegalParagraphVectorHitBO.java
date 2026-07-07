package com.laby.module.legal.service.embedding.bo;

import lombok.Data;

/**
 * 段落向量检索命中
 */
@Data
public class LegalParagraphVectorHitBO {

    private String paragraphId;

    private Double score;

}
