package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "合同版本条款 Diff 项")
@Data
public class LegalContractVersionDiffItemVO {

    private String clauseId;

    private String clauseTitle;

    @Schema(description = "MODIFIED | ADDED | REMOVED | UNCHANGED")
    private String changeType;

    private String beforeText;

    private String afterText;

    private List<Long> relatedOpinionIds = new ArrayList<>();

}
