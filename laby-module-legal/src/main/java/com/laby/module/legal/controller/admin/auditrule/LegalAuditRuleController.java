package com.laby.module.legal.controller.admin.auditrule;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRulePageReqVO;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRuleRespVO;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRuleSaveReqVO;
import com.laby.module.legal.service.auditrule.LegalAuditRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.laby.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 法务全局审核规则 Controller
 */
@Tag(name = "管理后台 - 法务全局审核规则")
@RestController
@RequestMapping("/legal/audit-rule")
@Validated
public class LegalAuditRuleController {

    @Resource
    private LegalAuditRuleService auditRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建审核规则")
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:create')")
    public CommonResult<Long> createAuditRule(@Valid @RequestBody LegalAuditRuleSaveReqVO createReqVO) {
        return success(auditRuleService.createAuditRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新审核规则")
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:update')")
    public CommonResult<Boolean> updateAuditRule(@Valid @RequestBody LegalAuditRuleSaveReqVO updateReqVO) {
        auditRuleService.updateAuditRule(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-enabled")
    @Operation(summary = "更新审核规则启用状态")
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:update')")
    public CommonResult<Boolean> updateAuditRuleEnabled(@RequestParam("id") Long id,
                                                        @RequestParam("enabled") Boolean enabled) {
        auditRuleService.updateAuditRuleEnabled(id, enabled);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除审核规则")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:delete')")
    public CommonResult<Boolean> deleteAuditRule(@RequestParam("id") Long id) {
        auditRuleService.deleteAuditRule(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除审核规则")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:delete')")
    public CommonResult<Boolean> deleteAuditRuleList(@RequestParam("ids") List<Long> ids) {
        auditRuleService.deleteAuditRuleList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得审核规则")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:query')")
    public CommonResult<LegalAuditRuleRespVO> getAuditRule(@RequestParam("id") Long id) {
        return success(auditRuleService.getAuditRule(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得审核规则分页")
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:query')")
    public CommonResult<PageResult<LegalAuditRuleRespVO>> getAuditRulePage(
            @Valid LegalAuditRulePageReqVO pageReqVO) {
        return success(auditRuleService.getAuditRulePage(pageReqVO));
    }

}
