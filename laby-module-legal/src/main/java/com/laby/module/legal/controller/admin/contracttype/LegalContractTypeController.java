package com.laby.module.legal.controller.admin.contracttype;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigOverviewRespVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigResolveRespVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypePageReqVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeRespVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeSaveReqVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeSimpleRespVO;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.service.contracttype.LegalContractTypeConfigService;
import com.laby.module.legal.service.contracttype.LegalContractTypeService;
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
 * 管理后台 - 法务合同类型 Controller
 */
@Tag(name = "管理后台 - 法务合同类型")
@RestController
@RequestMapping("/legal/contract-type")
@Validated
public class LegalContractTypeController {

    @Resource
    private LegalContractTypeService contractTypeService;
    @Resource
    private LegalContractTypeConfigService contractTypeConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建合同类型")
    @PreAuthorize("@ss.hasPermission('legal:contract-type:create')")
    public CommonResult<Long> createContractType(@Valid @RequestBody LegalContractTypeSaveReqVO createReqVO) {
        return success(contractTypeService.createContractType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同类型")
    @PreAuthorize("@ss.hasPermission('legal:contract-type:update')")
    public CommonResult<Boolean> updateContractType(@Valid @RequestBody LegalContractTypeSaveReqVO updateReqVO) {
        contractTypeService.updateContractType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:contract-type:delete')")
    public CommonResult<Boolean> deleteContractType(@RequestParam("id") Long id) {
        contractTypeService.deleteContractType(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除合同类型")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('legal:contract-type:delete')")
    public CommonResult<Boolean> deleteContractTypeList(@RequestParam("ids") List<Long> ids) {
        contractTypeService.deleteContractTypeList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:contract-type:query')")
    public CommonResult<LegalContractTypeRespVO> getContractType(@RequestParam("id") Long id) {
        LegalContractTypeDO contractType = contractTypeService.getContractType(id);
        return success(BeanUtils.toBean(contractType, LegalContractTypeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同类型分页")
    @PreAuthorize("@ss.hasPermission('legal:contract-type:query')")
    public CommonResult<PageResult<LegalContractTypeRespVO>> getContractTypePage(
            @Valid LegalContractTypePageReqVO pageReqVO) {
        PageResult<LegalContractTypeDO> pageResult = contractTypeService.getContractTypePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, LegalContractTypeRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得合同类型精简列表")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<List<LegalContractTypeSimpleRespVO>> getContractTypeSimpleList() {
        List<LegalContractTypeDO> list = contractTypeService.getContractTypeSimpleList();
        return success(BeanUtils.toBean(list, LegalContractTypeSimpleRespVO.class));
    }

    @GetMapping("/config-overview")
    @Operation(summary = "合同类型配置中枢概览（CFG-001）")
    @PreAuthorize("@ss.hasPermission('legal:contract-type:query')")
    public CommonResult<LegalContractTypeConfigOverviewRespVO> getConfigOverview(
            @RequestParam("id") Long id) {
        return success(contractTypeConfigService.getConfigOverview(id));
    }

    @GetMapping("/resolve-config")
    @Operation(summary = "模拟解析合同类型运行时 AI 配置（CFG-001）")
    @PreAuthorize("@ss.hasPermission('legal:contract-type:query')")
    public CommonResult<LegalContractTypeConfigResolveRespVO> resolveConfig(
            @RequestParam("contractTypeId") Long contractTypeId) {
        return success(contractTypeConfigService.resolveConfig(contractTypeId));
    }

}
