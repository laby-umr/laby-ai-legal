package com.laby.module.legal.controller.admin.contract;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactSaveReqVO;
import com.laby.module.legal.service.memory.LegalUserFactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.laby.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 法务用户事实记忆")
@RestController
@RequestMapping("/legal/contract/user-fact")
@Validated
public class LegalUserFactController {

    @Resource
    private LegalUserFactService userFactService;

    @GetMapping("/page")
    @Operation(summary = "用户事实记忆分页")
    @PreAuthorize("@ss.hasPermission('legal:contract-memory:query')")
    public CommonResult<PageResult<LegalUserFactRespVO>> getUserFactPage(
            @Valid LegalUserFactPageReqVO pageReqVO) {
        return success(userFactService.getUserFactPage(pageReqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "新增用户事实记忆")
    @PreAuthorize("@ss.hasPermission('legal:contract-memory:query')")
    public CommonResult<Long> createUserFact(@Valid @RequestBody LegalUserFactSaveReqVO reqVO) {
        return success(userFactService.createUserFact(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户事实记忆")
    @PreAuthorize("@ss.hasPermission('legal:contract-memory:query')")
    public CommonResult<Boolean> updateUserFact(@Valid @RequestBody LegalUserFactSaveReqVO reqVO) {
        userFactService.updateUserFact(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户事实记忆")
    @PreAuthorize("@ss.hasPermission('legal:contract-memory:query')")
    public CommonResult<Boolean> deleteUserFact(
            @Parameter(description = "事实编号", required = true) @RequestParam("id") Long id) {
        userFactService.deleteUserFact(id);
        return success(true);
    }

}
