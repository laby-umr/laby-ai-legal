package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "合同版本 Diff Response")
@Data
public class LegalContractVersionDiffRespVO {

    private Long contractId;

    private Long fromVersionId;

    private Long toVersionId;

    private Integer fromVersionNo;

    private Integer toVersionNo;

    private List<LegalContractVersionDiffItemVO> diffs = new ArrayList<>();

}
