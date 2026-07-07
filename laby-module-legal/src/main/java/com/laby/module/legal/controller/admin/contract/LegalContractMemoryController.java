package com.laby.module.legal.controller.admin.contract;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemoryPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemoryRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemorySaveReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactSaveReqVO;
import com.laby.module.legal.dal.dataobject.memory.LegalContractMemoryDO;
import com.laby.module.legal.enums.memory.LegalContractMemoryTypeEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.memory.LegalContractEpisodicMemoryService;
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

import java.util.List;

import static com.laby.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 法务合同情节记忆")
@RestController
@RequestMapping("/legal/contract/memory")
@Validated
public class LegalContractMemoryController {

    @Resource
    private LegalContractEpisodicMemoryService episodicMemoryService;
    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalUserFactService userFactService;

    @GetMapping("/page")
    @Operation(summary = "合同情节记忆分页")
    @PreAuthorize("@ss.hasPermission('legal:contract-memory:query')")
    public CommonResult<PageResult<LegalContractMemoryRespVO>> getMemoryPage(
            @Valid LegalContractMemoryPageReqVO pageReqVO) {
        return success(episodicMemoryService.getMemoryPage(pageReqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "合同情节记忆列表")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<List<LegalContractMemoryRespVO>> listMemories(
            @Parameter(description = "合同编号", required = true) @RequestParam("contractId") Long contractId,
            @Parameter(description = "会话编号，可选；传入则只返回该会话记忆")
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        contractService.validateContractExists(contractId);
        List<LegalContractMemoryDO> memories = episodicMemoryService.listMemories(contractId, sessionId);
        return success(BeanUtils.toBean(memories, LegalContractMemoryRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新增合同情节记忆")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Long> createMemory(@Valid @RequestBody LegalContractMemorySaveReqVO reqVO) {
        contractService.validateContractExists(reqVO.getContractId());
        if (LegalContractMemoryTypeEnum.FACT.getType().equals(reqVO.getMemoryType())) {
            LegalUserFactSaveReqVO factReq = new LegalUserFactSaveReqVO();
            factReq.setUserId(SecurityFrameworkUtils.getLoginUserId());
            factReq.setContractId(reqVO.getContractId());
            factReq.setSessionId(reqVO.getSessionId());
            factReq.setContent(reqVO.getContent());
            return success(userFactService.createUserFact(factReq));
        }
        Long id = episodicMemoryService.createMemory(
                reqVO.getContractId(), reqVO.getSessionId(), reqVO.getMemoryType(), reqVO.getContent());
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "修改合同情节记忆")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Boolean> updateMemory(@Valid @RequestBody LegalContractMemorySaveReqVO reqVO) {
        contractService.validateContractExists(reqVO.getContractId());
        episodicMemoryService.updateMemory(
                reqVO.getId(), reqVO.getContractId(), reqVO.getMemoryType(), reqVO.getContent());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同情节记忆")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Boolean> deleteMemory(
            @Parameter(description = "记忆编号", required = true) @RequestParam("id") Long id,
            @Parameter(description = "合同编号", required = true) @RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        episodicMemoryService.deleteMemory(id, contractId);
        return success(true);
    }

}
