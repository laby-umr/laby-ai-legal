package com.laby.module.legal.controller.admin.skillpack;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackLegalToolRespVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackPageReqVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackRespVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackSaveReqVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackSimpleRespVO;
import com.laby.module.legal.service.skillpack.LegalSkillPackService;
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
 * 管理后台 - 法务 AI 技能包
 * <p>
 * 仅负责参数校验、权限控制与 HTTP 适配；业务逻辑下沉至 Service 层。
 */
@Tag(name = "管理后台 - 法务 AI 技能包")
@RestController
@RequestMapping("/legal/skill-pack")
@Validated
public class LegalSkillPackController {

    @Resource
    private LegalSkillPackService skillPackService;

    @PostMapping("/create")
    @Operation(summary = "创建 AI 技能包")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:create')")
    public CommonResult<Long> createSkillPack(@Valid @RequestBody LegalSkillPackSaveReqVO createReqVO) {
        return success(skillPackService.createSkillPack(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 AI 技能包")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:update')")
    public CommonResult<Boolean> updateSkillPack(@Valid @RequestBody LegalSkillPackSaveReqVO updateReqVO) {
        skillPackService.updateSkillPack(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-enabled")
    @Operation(summary = "更新 AI 技能包启用状态")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:update')")
    public CommonResult<Boolean> updateSkillPackEnabled(@RequestParam("id") Long id,
                                                        @RequestParam("enabled") Boolean enabled) {
        skillPackService.updateSkillPackEnabled(id, enabled);
        return success(true);
    }

    @PostMapping("/copy")
    @Operation(summary = "复制 AI 技能包")
    @Parameter(name = "id", description = "源技能包编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:create')")
    public CommonResult<Long> copySkillPack(@RequestParam("id") Long id) {
        return success(skillPackService.copySkillPack(id));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 AI 技能包")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:delete')")
    public CommonResult<Boolean> deleteSkillPack(@RequestParam("id") Long id) {
        skillPackService.deleteSkillPack(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除 AI 技能包")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:delete')")
    public CommonResult<Boolean> deleteSkillPackList(@RequestParam("ids") List<Long> ids) {
        skillPackService.deleteSkillPackList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 AI 技能包")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:query')")
    public CommonResult<LegalSkillPackRespVO> getSkillPack(@RequestParam("id") Long id) {
        return success(skillPackService.getSkillPack(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 AI 技能包分页")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:query')")
    public CommonResult<PageResult<LegalSkillPackRespVO>> getSkillPackPage(
            @Valid LegalSkillPackPageReqVO pageReqVO) {
        return success(skillPackService.getSkillPackPage(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "AI 技能包精简列表")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:query')")
    public CommonResult<List<LegalSkillPackSimpleRespVO>> getSkillPackSimpleList(
            @RequestParam(value = "scene", required = false) String scene) {
        return success(skillPackService.getSkillPackSimpleList(scene));
    }

    @GetMapping("/legal-agent-tools")
    @Operation(summary = "法务 Agent 可选工具列表（CFG-001）")
    @PreAuthorize("@ss.hasPermission('legal:skill-pack:query')")
    public CommonResult<List<LegalSkillPackLegalToolRespVO>> getLegalAgentTools() {
        return success(skillPackService.getLegalAgentToolOptions());
    }

}
