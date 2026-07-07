package com.laby.module.legal.service.contract.bo;

import com.laby.module.legal.enums.clause.LegalClauseTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析层条款单元（内存模型）
 */
@Data
@Builder
public class LegalClauseUnitBO {

    private String clauseId;
    private String parentClauseId;
    private Integer sort;
    private String title;
    private Integer level;
    private LegalClauseTypeEnum type;
    private String path;
    @Builder.Default
    private List<String> paragraphIds = new ArrayList<>();
    private String fullText;

}
