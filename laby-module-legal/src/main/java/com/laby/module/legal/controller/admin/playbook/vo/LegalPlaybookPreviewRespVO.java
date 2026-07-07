package com.laby.module.legal.controller.admin.playbook.vo;

import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Playbook 预览 Response")
@Data
public class LegalPlaybookPreviewRespVO {

    private Long contractTypeId;
    private LegalReviewPlanBO plan;

}
