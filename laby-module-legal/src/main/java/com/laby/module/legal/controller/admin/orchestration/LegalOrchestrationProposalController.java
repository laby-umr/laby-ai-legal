package com.laby.module.legal.controller.admin.orchestration;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractAgentProposalReqVO;
import com.laby.module.legal.controller.admin.orchestration.vo.LegalOrchestrationProposalRespVO;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;
import com.laby.module.legal.service.agent.LegalAgentProposalService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.laby.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 法务 AI 编排提案")
@RestController
@RequestMapping("/legal/orchestration/proposal")
@Validated
public class LegalOrchestrationProposalController {

    @Resource
    private LegalOrchestrationProposalService orchestrationProposalService;
    @Resource
    private LegalAgentProposalService agentProposalService;

    @GetMapping("/list-pending")
    @Operation(summary = "查询对话下待处理编排提案")
    @PreAuthorize("@ss.hasPermission('legal:orchestration:query') or @ss.hasPermission('legal:contract:query')")
    public CommonResult<List<LegalOrchestrationProposalRespVO>> listPending(
            @Parameter(description = "AI 对话编号", required = true) @RequestParam("conversationId") Long conversationId) {
        List<LegalAgentProposalDO> list = orchestrationProposalService.listPendingByConversationId(conversationId);
        return success(BeanUtils.toBean(list, LegalOrchestrationProposalRespVO.class));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行编排提案")
    @PreAuthorize("@ss.hasPermission('legal:orchestration:execute') or @ss.hasPermission('legal:contract:create')")
    public CommonResult<Boolean> executeProposal(@Valid @RequestBody LegalContractAgentProposalReqVO reqVO) {
        agentProposalService.executeProposal(reqVO.getProposalNo());
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消编排提案")
    @PreAuthorize("@ss.hasPermission('legal:orchestration:execute') or @ss.hasPermission('legal:contract:create')")
    public CommonResult<Boolean> cancelProposal(@Valid @RequestBody LegalContractAgentProposalReqVO reqVO) {
        agentProposalService.cancelProposal(reqVO.getProposalNo());
        return success(true);
    }

}
