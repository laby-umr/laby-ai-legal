package com.laby.module.legal.controller.admin.playbook.vo;

import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Playbook 模拟运行 Response")
@Data
public class LegalPlaybookSimulateRespVO {

    private Long contractId;
    private Integer deterministicCount;
    private List<LegalAuditOpinionDraftBO> opinions = new ArrayList<>();

}
