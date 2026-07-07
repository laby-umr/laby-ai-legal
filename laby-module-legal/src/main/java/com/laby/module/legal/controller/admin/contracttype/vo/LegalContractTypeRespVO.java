package com.laby.module.legal.controller.admin.contracttype.vo;

import com.laby.framework.excel.core.annotations.DictFormat;
import com.laby.module.system.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同类型 Response VO")
@Data
public class LegalContractTypeRespVO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Long knowledgeId;
    private Long defaultSkillPackIdAudit;
    private Long defaultSkillPackIdChat;
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;

}
