package com.laby.module.legal.controller.admin.playbook;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.module.legal.controller.admin.playbook.vo.LegalPlaybookPreviewRespVO;
import com.laby.module.legal.controller.admin.playbook.vo.LegalPlaybookSimulateRespVO;
import com.laby.module.legal.service.playbook.LegalPlaybookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.laby.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 法务 Playbook")
@RestController
@RequestMapping("/legal/playbook")
@Validated
public class LegalPlaybookController {

    @Resource
    private LegalPlaybookService playbookService;

    @GetMapping("/preview")
    @Operation(summary = "预览 ReviewPlan")
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:query')")
    public CommonResult<LegalPlaybookPreviewRespVO> preview(
            @RequestParam(value = "contractTypeId", required = false) Long contractTypeId) {
        return success(playbookService.preview(contractTypeId));
    }

    @PostMapping("/simulate")
    @Operation(summary = "模拟运行确定性 Playbook（不落库）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalPlaybookSimulateRespVO> simulate(@RequestParam("contractId") Long contractId) {
        return success(playbookService.simulate(contractId));
    }

}
