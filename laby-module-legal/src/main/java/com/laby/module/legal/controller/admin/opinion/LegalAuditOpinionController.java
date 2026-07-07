package com.laby.module.legal.controller.admin.opinion;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.opinion.vo.LegalAuditOpinionBatchReqVO;
import com.laby.module.legal.controller.admin.opinion.vo.LegalAuditOpinionRespVO;
import com.laby.module.legal.controller.admin.opinion.vo.LegalAuditOpinionSaveReqVO;
import com.laby.module.legal.controller.admin.opinion.vo.LegalOpinionDocumentApplyResultVO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.service.opinion.LegalAuditOpinionService;
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
 * 管理后台 - 法务审核意见 Controller
 */
@Tag(name = "管理后台 - 法务审核意见")
@RestController
@RequestMapping("/legal/opinion")
@Validated
public class LegalAuditOpinionController {

    @Resource
    private LegalAuditOpinionService opinionService;

    @GetMapping("/list-by-contract")
    @Operation(summary = "按合同获得意见列表")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<List<LegalAuditOpinionRespVO>> getOpinionList(
            @Parameter(description = "合同编号", required = true) @RequestParam("contractId") Long contractId) {
        List<LegalAuditOpinionDO> list = opinionService.getOpinionListByContractId(contractId);
        return success(BeanUtils.toBean(list, LegalAuditOpinionRespVO.class));
    }

    @PutMapping("/adopt")
    @Operation(summary = "采纳意见")
    @Parameter(name = "id", description = "意见编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<LegalOpinionDocumentApplyResultVO> adoptOpinion(@RequestParam("id") Long id) {
        return success(opinionService.adopt(id));
    }

    @PutMapping("/ignore")
    @Operation(summary = "忽略意见")
    @Parameter(name = "id", description = "意见编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> ignoreOpinion(@RequestParam("id") Long id) {
        opinionService.ignore(id);
        return success(true);
    }

    @PutMapping("/revoke")
    @Operation(summary = "撤销处置（恢复待处置）")
    @Parameter(name = "id", description = "意见编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<LegalOpinionDocumentApplyResultVO> revokeOpinion(@RequestParam("id") Long id) {
        return success(opinionService.revoke(id));
    }

    @PutMapping("/batch-adopt")
    @Operation(summary = "批量采纳意见")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<LegalOpinionDocumentApplyResultVO> batchAdoptOpinion(@Valid @RequestBody LegalAuditOpinionBatchReqVO reqVO) {
        return success(opinionService.batchAdopt(reqVO.getIds()));
    }

    @PutMapping("/batch-ignore")
    @Operation(summary = "批量忽略意见")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> batchIgnoreOpinion(@Valid @RequestBody LegalAuditOpinionBatchReqVO reqVO) {
        opinionService.batchIgnore(reqVO.getIds());
        return success(true);
    }

    @PostMapping("/create-manual")
    @Operation(summary = "手工新增意见")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Long> createManualOpinion(@Valid @RequestBody LegalAuditOpinionSaveReqVO reqVO) {
        return success(opinionService.createManual(reqVO));
    }

    @PutMapping("/update-manual")
    @Operation(summary = "更新手工意见")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> updateManualOpinion(@Valid @RequestBody LegalAuditOpinionSaveReqVO reqVO) {
        opinionService.updateManual(reqVO);
        return success(true);
    }

}
