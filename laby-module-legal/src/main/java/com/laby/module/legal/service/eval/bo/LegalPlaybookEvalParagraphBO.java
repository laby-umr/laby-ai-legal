package com.laby.module.legal.service.eval.bo;

import lombok.Data;

/**
 * Playbook 评测段落夹具
 */
@Data
public class LegalPlaybookEvalParagraphBO {

    /** 段落编号，如 p-1 */
    private String paragraphId;

    /** 段落正文 */
    private String text;

}
