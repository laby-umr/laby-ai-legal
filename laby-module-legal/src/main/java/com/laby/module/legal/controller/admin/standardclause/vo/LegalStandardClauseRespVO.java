package com.laby.module.legal.controller.admin.standardclause.vo;

import com.laby.framework.excel.core.annotations.DictFormat;
import com.laby.module.legal.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.laby.module.system.enums.DictTypeConstants.COMMON_STATUS;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 标准条款 Response VO")
@Data
public class LegalStandardClauseRespVO {

    private Long id;
    private String name;
    @DictFormat(DictTypeConstants.LEGAL_CLAUSE_TYPE)
    private String clauseType;
    @DictFormat(DictTypeConstants.LEGAL_STANDARD_CLAUSE_SCOPE)
    private String categoryScope;
    private String content;
    private String referenceSource;
    @DictFormat(COMMON_STATUS)
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;

}
