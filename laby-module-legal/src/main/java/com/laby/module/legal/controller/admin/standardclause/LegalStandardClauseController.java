package com.laby.module.legal.controller.admin.standardclause;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClausePageReqVO;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClauseRespVO;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClauseSaveReqVO;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClauseSimpleRespVO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import com.laby.module.legal.service.standardclause.LegalStandardClauseService;
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
 * 管理后台 - 法务标准条款库 Controller
 */
@Tag(name = "管理后台 - 法务标准条款库")
@RestController
@RequestMapping("/legal/standard-clause")
@Validated
public class LegalStandardClauseController {

    @Resource
    private LegalStandardClauseService standardClauseService;

    @PostMapping("/create")
    @Operation(summary = "创建标准条款")
    @PreAuthorize("@ss.hasPermission('legal:standard-clause:create')")
    public CommonResult<Long> createStandardClause(@Valid @RequestBody LegalStandardClauseSaveReqVO createReqVO) {
        return success(standardClauseService.createStandardClause(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新标准条款")
    @PreAuthorize("@ss.hasPermission('legal:standard-clause:update')")
    public CommonResult<Boolean> updateStandardClause(@Valid @RequestBody LegalStandardClauseSaveReqVO updateReqVO) {
        standardClauseService.updateStandardClause(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除标准条款")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:standard-clause:delete')")
    public CommonResult<Boolean> deleteStandardClause(@RequestParam("id") Long id) {
        standardClauseService.deleteStandardClause(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除标准条款")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('legal:standard-clause:delete')")
    public CommonResult<Boolean> deleteStandardClauseList(@RequestParam("ids") List<Long> ids) {
        standardClauseService.deleteStandardClauseList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得标准条款")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:standard-clause:query')")
    public CommonResult<LegalStandardClauseRespVO> getStandardClause(@RequestParam("id") Long id) {
        LegalStandardClauseDO clause = standardClauseService.getStandardClause(id);
        return success(BeanUtils.toBean(clause, LegalStandardClauseRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得标准条款分页")
    @PreAuthorize("@ss.hasPermission('legal:standard-clause:query')")
    public CommonResult<PageResult<LegalStandardClauseRespVO>> getStandardClausePage(
            @Valid LegalStandardClausePageReqVO pageReqVO) {
        PageResult<LegalStandardClauseDO> pageResult = standardClauseService.getStandardClausePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, LegalStandardClauseRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得标准条款精简列表")
    @PreAuthorize("@ss.hasPermission('legal:audit-rule:query')")
    public CommonResult<List<LegalStandardClauseSimpleRespVO>> getStandardClauseSimpleList() {
        List<LegalStandardClauseDO> list = standardClauseService.getStandardClauseSimpleList();
        return success(BeanUtils.toBean(list, LegalStandardClauseSimpleRespVO.class));
    }

}
