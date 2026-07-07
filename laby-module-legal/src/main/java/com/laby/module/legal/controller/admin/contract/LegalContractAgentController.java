package com.laby.module.legal.controller.admin.contract;

import com.laby.framework.ratelimiter.core.annotation.RateLimiter;
import com.laby.framework.ratelimiter.core.keyresolver.impl.UserRateLimiterKeyResolver;
import com.laby.module.legal.service.agent.LegalContractAgentResumeService;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractAgentConfirmReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatRespVO;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractAgentProposalReqVO;
import com.laby.module.legal.service.agent.LegalContractAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static com.laby.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 法务合同 Agent 提案 Controller
 */
@Tag(name = "管理后台 - 法务合同 Agent")
@RestController
@RequestMapping("/legal/contract/agent")
@Validated
public class LegalContractAgentController {

    @Resource
    private LegalContractAgentService contractAgentService;
    @Resource
    private LegalContractAgentResumeService agentResumeService;

    @PostMapping("/confirm")
    @Operation(summary = "Agent Permission Confirm（同步）")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> confirm(@Valid @RequestBody LegalContractAgentConfirmReqVO reqVO) {
        agentResumeService.confirm(reqVO);
        return success(true);
    }

    @PostMapping(value = "/confirm-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Agent Permission Confirm（SSE 续流）")
    @RateLimiter(time = 60, count = 10, keyResolver = UserRateLimiterKeyResolver.class)
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public Flux<CommonResult<LegalContractChatRespVO>> confirmStream(
            @Valid @RequestBody LegalContractAgentConfirmReqVO reqVO) {
        return agentResumeService.resumeStream(reqVO);
    }

    @PostMapping("/proposal/execute")
    @Operation(summary = "执行 Agent 写操作提案")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> executeProposal(@Valid @RequestBody LegalContractAgentProposalReqVO reqVO) {
        contractAgentService.executeProposal(reqVO.getProposalNo());
        return success(true);
    }

    @PostMapping("/proposal/cancel")
    @Operation(summary = "取消 Agent 写操作提案")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> cancelProposal(@Valid @RequestBody LegalContractAgentProposalReqVO reqVO) {
        contractAgentService.cancelProposal(reqVO.getProposalNo());
        return success(true);
    }

}
